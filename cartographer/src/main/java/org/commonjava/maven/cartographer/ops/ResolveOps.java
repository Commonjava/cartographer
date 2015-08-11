/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectVersionReferences;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.AndFilter;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.request.*;
import org.commonjava.maven.cartographer.ops.fn.FunctionInputSelector;
import org.commonjava.maven.cartographer.ops.fn.GraphFunction;
import org.commonjava.maven.cartographer.ops.fn.MultiGraphFunction;
import org.commonjava.maven.cartographer.request.AbstractGraphRequest;
import org.commonjava.maven.cartographer.util.RecipeResolver;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ResolveOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected DiscoverySourceManager sourceManager;

    @Inject
    protected ProjectRelationshipDiscoverer discoverer;

    @Inject
    protected GraphAggregator aggregator;

    @Inject
    protected ArtifactManager artifacts;

    @Inject
    protected MultiGraphCalculator calculations;

    @Inject
    protected RelationshipGraphFactory graphFactory;

    @Inject
    protected MavenPomReader pomReader;

    @Inject
    protected RecipeResolver recipeResolver;

    @Inject
    @ExecutorConfig( daemon = true, named = "carto-resolve-ops", priority = 9, threads = 16 )
    private ExecutorService executor;

    protected ResolveOps()
    {
    }

    public ResolveOps( final MultiGraphCalculator calculations, final DiscoverySourceManager sourceManager,
                       final ProjectRelationshipDiscoverer discoverer, final GraphAggregator aggregator,
                       final ArtifactManager artifacts, final ExecutorService executor,
                       final RelationshipGraphFactory graphFactory, final RecipeResolver dtoResolver )
    {
        this.calculations = calculations;
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregator = aggregator;
        this.artifacts = artifacts;
        this.executor = executor;
        this.graphFactory = graphFactory;
        this.recipeResolver = dtoResolver;
    }

    public void resolveAndExtractSingleGraph( final ProjectRelationshipFilter filter,
                                              final SingleGraphRequest recipe, final GraphFunction extractor )
                    throws CartoDataException, CartoRequestException
    {
        if ( filter != null && filter != AnyFilter.INSTANCE )
        {
            for ( final GraphDescription desc : recipe.getGraphComposition() )
            {
                desc.setFilter( new AndFilter( filter, desc.filter() ) );
            }
        }

        final Map<GraphDescription, RelationshipGraph> graphMap = resolveToGraphMap( recipe );
        try
        {
            final GraphDescription original = recipe.getGraph();
            final RelationshipGraph graph = graphMap.get( original );
            extractor.extract( graph );
        }
        finally
        {
            graphMap.values().forEach( IOUtils::closeQuietly );
        }
    }

    public <T> void resolveAndExtractMultiGraph( final ProjectRelationshipFilter filter,
                                                 final MultiGraphRequest recipe,
                                                 final FunctionInputSelector<T> selector,
                                                 final MultiGraphFunction<T> extractor )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        if ( recipe.getGraphComposition()
                   .getGraphs()
                   .isEmpty() )
        {
            throw new CartoDataException( "No graph descriptions provided! Cannot continue." );
        }

        if ( filter != null && filter != AnyFilter.INSTANCE )
        {
            for ( final GraphDescription desc : recipe.getGraphComposition() )
            {
                desc.setFilter( new AndFilter( filter, desc.filter() ) );
            }
        }

        final Map<GraphDescription, RelationshipGraph> graphMap = resolveToGraphMap( recipe );
        try
        {
            Supplier<Set<ProjectVersionRef>> allProjects;
            Supplier<Set<ProjectRelationship<?>>> allRels;
            Supplier<Set<ProjectVersionRef>> roots;

            final GraphComposition comp = recipe.getGraphComposition();
            if ( comp.getGraphs()
                     .size() < 2 )
            {
                final GraphDescription desc = comp.getGraphs()
                                                  .get( 0 );

                final RelationshipGraph graph = graphMap.get( desc );

                allProjects = graph::getAllProjects;
                allRels = graph::getAllRelationships;
                roots = graph::getRoots;
            }
            else
            {
                if ( comp.getGraphs()
                         .size() > 1 && comp.getCalculation() == null )
                {
                    comp.setCalculation( GraphCalculationType.ADD );
                }

                final GraphCalculation calcResult = calculations.calculateFromGraphMap( comp, graphMap );
                allProjects = calcResult::getResultingProjects;
                allRels = calcResult::getResultingRelationships;
                roots = calcResult::getResultingRoots;
            }

            final T result = selector.select( allProjects, allRels, roots );
            extractor.extract( result, graphMap );
        }
        finally
        {
            graphMap.values().forEach( IOUtils::closeQuietly );
        }
    }

    /**
     * Resolve any variable versions in the specified root GAVs of the {@link GraphComposition} embeded in the 
     * {@link AbstractGraphRequest}. Then retrieve, and if configured, discover missing parts of the relationship
     * graph. Return a mapping of {@link GraphDescription} to the {@link ViewParams} used during resolution, 
     * which might contain root GAVs different than those given in the original {@link AbstractGraphRequest} due to
     * potential root GAV differences from resolution of variable versions.
     */
    public LinkedHashMap<GraphDescription, ViewParams> resolveToParamMap( final AbstractGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        final LinkedHashMap<GraphDescription, ViewParams> result = new LinkedHashMap<>();
        for ( final GraphDescription desc : recipe.getGraphComposition() )
        {
            if ( recipe.isResolve() )
            {
                resolveGraph( desc, recipe, ( graph ) -> {
                    result.put( desc, graph.getParams() );
                    IOUtils.closeQuietly( graph );
                } );
            }
            else
            {
                final ViewParams params =
                    new ViewParams.Builder( recipe.getWorkspaceId(), desc.rootsArray() ).withFilter( desc.filter() )
                                                                                        .withMutator( new ManagedDependencyMutator() )
                                                                                        .withSelections( recipe.getVersionSelections() )
                                                                                        .build();
                // ensure the graph is available.
                RelationshipGraph graph = null;
                try
                {
                    graph = graphFactory.open( params, false );
                    result.put( desc, graph.getParams() );
                }
                catch ( final RelationshipGraphException e )
                {
                    throw new CartoDataException( "Failed to open existing graph: %s. Reason: %s", e, params,
                                                  e.getMessage() );
                }
                finally
                {
                    IOUtils.closeQuietly( graph );
                }
            }
        }

        return result;
    }

    /**
     * Resolve any variable versions in the specified root GAVs of the {@link GraphComposition} embeded in the 
     * {@link AbstractGraphRequest}. Then retrieve, and if configured, discover missing parts of the relationship
     * graph. Return a mapping of {@link GraphDescription} to the {@link ViewParams} used during resolution, 
     * which might contain root GAVs different than those given in the original {@link AbstractGraphRequest} due to
     * potential root GAV differences from resolution of variable versions.
     */
    public LinkedHashMap<GraphDescription, RelationshipGraph> resolveToGraphMap( final AbstractGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        final LinkedHashMap<GraphDescription, RelationshipGraph> result = new LinkedHashMap<>();
        for ( final GraphDescription desc : recipe.getGraphComposition() )
        {
            resolveGraph( desc, recipe, ( graph ) -> result.put( desc, graph ) );
        }

        return result;
    }

    /**
     * Resolve any variable versions in the specified root GAVs of the {@link GraphComposition} embeded in the 
     * {@link AbstractGraphRequest}. Then retrieve, and if configured, discover missing parts of the relationship
     * graph. Return a mapping of {@link GraphDescription} to the {@link ViewParams} used during resolution, 
     * which might contain root GAVs different than those given in the original {@link AbstractGraphRequest} due to
     * potential root GAV differences from resolution of variable versions.
     */
    public void resolveGraphs( final AbstractGraphRequest recipe, final Consumer<RelationshipGraph> consumer )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        for ( final GraphDescription desc : recipe.getGraphComposition() )
        {
            resolveGraph( desc, recipe, consumer );
        }
    }

    /**
     * Resolve any variable versions in the specified root GAVs, retrieve, and if configured, discover missing parts of the relationship
     * graph. Return the {@link ViewParams} instance resulting from configuration via the given {@link AggregationOptions} and the root GAVs with
     * potential root GAV differences due to resolution of variable versions. If autoClose parameter is false, then leave the graph open for 
     * subsequent reuse.
     * <br/>
     * <b>NOTE:</b> This method assumes {@link RecipeResolver#resolve(AbstractGraphRequest)} has already been called.
     */
    private void resolveGraph( final GraphDescription desc, final AbstractGraphRequest recipe,
                               final Consumer<RelationshipGraph> consumer )
                    throws CartoDataException, CartoRequestException
    {
        logger.info( "Initial source location: '{}'", recipe.getSourceLocation() );
        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation()
                                                                   .getUri() );

        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '{}'. Use the form: '{}' instead.",
                                          recipe.getSourceLocation(), sourceManager.getFormatHint() );
        }

        if ( !recipe.isResolve() )
        {
            final ViewParams params =
                new ViewParams.Builder( recipe.getWorkspaceId(), desc.rootsArray() ).withFilter( desc.filter() )
                                                                                    .withMutator( new ManagedDependencyMutator() )
                                                                                    .withSelections( recipe.getVersionSelections() )
                                                                                    .build();
            // ensure the graph is available.
            try
            {
                final RelationshipGraph graph = graphFactory.open( params, false );
                consumer.accept( graph );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Failed to open: %s. Reason: %s", e, params, e.getMessage() );
            }

            return;
        }

        final AggregationOptions aggOptions = createAggregationOptions( recipe, desc.filter() );
        final DiscoveryConfig discoveryConfig = recipe.getDiscoveryConfig();

        final List<ProjectVersionRef> specifics = new ArrayList<>();

        for ( final ProjectVersionRef root : desc.getRoots() )
        {
            ProjectVersionRef specific = discoverer.resolveSpecificVersion( root, discoveryConfig );
            if ( specific == null )
            {
                specific = root;
            }

            specifics.add( specific );
        }

        final ViewParams params =
            new ViewParams.Builder( recipe.getWorkspaceId(), specifics ).withFilter( aggOptions.getFilter() )
                                                                        .withMutator( aggOptions.getMutator() )
                                                                        .withSelections( recipe.getVersionSelections() )
                                                                        .build();

        sourceManager.activateWorkspaceSources( params, discoveryConfig.getLocations() );

        try
        {
            final RelationshipGraph graph = graphFactory.open( params, true );
            for ( final ProjectVersionRef root : specifics )
            {
                if ( !graph.containsGraph( root ) || graph.hasProjectError( root ) )
                {
                    try
                    {
                        graph.clearProjectError( root );
                    }
                    catch ( final RelationshipGraphException e )
                    {
                        logger.error( String.format( "Cannot clear project error for: %s in graph: %s. Reason: %s",
                                                     root, graph, e.getMessage() ), e );
                        continue;
                    }

                    if ( !aggOptions.isDiscoveryEnabled() )
                    {
                        logger.info( "Resolving direct relationships for root: {}", root );
                        final DiscoveryResult result = discoverer.discoverRelationships( root, graph, discoveryConfig );
                        logger.info( "Result: {} relationships", ( result == null ? 0
                                        : result.getAcceptedRelationships()
                                                .size() ) );
                    }
                }
            }

            if ( aggOptions.isDiscoveryEnabled() )
            {
                logger.info( "Performing graph discovery for: {}", specifics );
                aggregator.connectIncomplete( graph, aggOptions );
            }

            consumer.accept( graph );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to open/modify graph: {}. Reason: {}", e, params, e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveRepositoryContents( final RepositoryContentRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        if ( recipe == null || !recipe.isValid() )
        {
            throw new CartoDataException( "Repository content request is invalid: {}", recipe );
        }

        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation()
                                                                   .getUri() );
        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '{}'. Use the form: '{}' instead.",
                                          recipe.getSourceLocation(), sourceManager.getFormatHint() );
        }

        final Map<ProjectVersionRef, ProjectRefCollection> refMap = resolveReferenceMap( recipe );
        final List<RepoContentCollector> collectors = collectContent( refMap, recipe );

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> itemMap = new HashMap<>();
        for ( final RepoContentCollector collector : collectors )
        {
            final Map<ArtifactRef, ConcreteResource> items = collector.getItems();

            if ( items != null && !items.isEmpty() )
            {
                logger.debug( "{} Returning for: {}\n\n  {}", collector, collector.getRef(),
                              new JoinString( "\n  ", items.entrySet() ) );
                Map<ArtifactRef, ConcreteResource> existingItems = itemMap.get( collector.getRef() );
                if ( existingItems == null )
                {
                    itemMap.put( collector.getRef(), items );
                    existingItems = items;
                }
                else
                {
                    existingItems.putAll( items );
                }

                logger.debug( "{} Accumulated for: {}\n\n  {}", collector, collector.getRef(),
                              new JoinString( "\n  ", existingItems.entrySet() ) );
            }
            else
            {
                logger.warn( "{} No items returned for: {}", collector, collector.getRef() );
            }
        }

        return itemMap;
    }

    private List<RepoContentCollector> collectContent( final Map<ProjectVersionRef, ProjectRefCollection> refMap,
                                                       final RepositoryContentRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final Location location = recipe.getSourceLocation();
        final Set<Location> excluded = recipe.getExcludedSourceLocations();

        if ( excluded != null && excluded.contains( location ) )
        {
            // no sense in going through all the rest if everything is excluded...
            throw new CartoDataException( "RepositoryContentRequest is insane! Source location is among those excluded!" );
        }

        int projectCounter = 1;
        final int projectSz = refMap.size();
        final List<RepoContentCollector> collectors = new ArrayList<>( projectSz );

        final DiscoveryConfig dconf = recipe.getDiscoveryConfig();

        for ( final Map.Entry<ProjectVersionRef, ProjectRefCollection> entry : refMap.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final ProjectRefCollection refs = entry.getValue();

            final RepoContentCollector collector =
                new RepoContentCollector( ref, refs, recipe, location, dconf, artifacts, discoverer, excluded,
                                          projectCounter, projectSz );

            collectors.add( collector );

            projectCounter++;
        }

        final CountDownLatch latch = new CountDownLatch( collectors.size() );
        for ( final RepoContentCollector collector : collectors )
        {
            collector.setLatch( latch );
            executor.execute( collector );
        }

        // TODO: timeout with loop...
        while ( latch.getCount() > 0 )
        {
            logger.info( "Waiting for {} more content-collection threads to complete.", latch.getCount() );
            try
            {
                latch.await( 2, TimeUnit.SECONDS );
            }
            catch ( final InterruptedException e )
            {
                logger.error( "Abandoning repo-content assembly for: {}", recipe );
            }
        }

        return collectors;
    }

    /**
     * Discover the dependency graphs for the configured graph composition, and then traverse them to construct a mapping of GAV to set of references
     * that can be used to render various kinds of output. If the request contains injectedBOMs, then read the managed dependencies from these into
     * a mapping of GA -> GAV that we can pass into the {@link ViewParams} we'll eventually use to discover and traverse the graph.
     * <br/>
     * Returns null if {@link RepositoryContentRequest#setSourceLocation(Location)} hasn't
     * been called before this method is called.
     * <br/>
     * @throws {@link CartoDataException} if one or more of the request's injected BOMs cannot be resolved or if an
     * unexpected problem takes place during graph resolution or traversal.
     * @throws {@link CartoRequestException} if the request doesn't contain enough basic
     * info to be used (See: {@link RepositoryContentRequest#isValid()}) or if the source {@link Location} hasn't been set on the request
     */
    private Map<ProjectVersionRef, ProjectRefCollection> resolveReferenceMap( final RepositoryContentRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        logger.info( "Building repository for: {}", recipe );

        recipeResolver.resolve( recipe );

        final Map<ProjectVersionRef, ProjectRefCollection> refMap = new HashMap<>();
        final MultiGraphFunction<Set<ProjectRelationship<?>>> extractor = ( allRels, graphMap ) -> {
            try
            {
                refMap.putAll( collectProjectVersionReferences( allRels ) );

                for ( final RelationshipGraph graph : graphMap.values() )
                {
                    for ( final ProjectVersionRef root : graph.getRoots() )
                    {
                        ProjectRefCollection refCollection = refMap.get( root );
                        if ( refCollection == null )
                        {
                            refCollection = new ProjectRefCollection();
                            refCollection.addVersionRef( root );

                            refMap.put( root, refCollection );
                        }

                        if ( root instanceof ArtifactRef )
                        {
                            refCollection.addArtifactRef( (ArtifactRef) root );
                        }
                    }
                }
            }
            finally
            {
                for ( final RelationshipGraph graph : graphMap.values() )
                {
                    IOUtils.closeQuietly( graph );
                }
            }
        };

        resolveAndExtractMultiGraph( AnyFilter.INSTANCE, recipe, ( allRefs, allRels, roots ) -> allRels.get(),
                                     extractor );

        return refMap;
    }

    private AggregationOptions createAggregationOptions( final AbstractGraphRequest recipe,
                                                         final ProjectRelationshipFilter baseFilter )
                    throws CartoDataException, CartoRequestException
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();
        options.setDiscoveryEnabled( recipe.isResolve() );
        options.setFilter( recipe.buildFilter( baseFilter ) );

        options.setDiscoveryConfig( recipe.getDiscoveryConfig() );

        options.setProcessIncompleteSubgraphs( true );
        options.setProcessVariableSubgraphs( true );

        return options;
    }

}
