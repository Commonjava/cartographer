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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.PathsTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.dto.AbstractResolverRecipe;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.cartographer.dto.resolve.DTOResolver;
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
    protected CalculationOps calculations;

    @Inject
    protected RelationshipGraphFactory graphFactory;

    @Inject
    protected MavenPomReader pomReader;

    @Inject
    protected DTOResolver dtoResolver;

    @Inject
    @ExecutorConfig( daemon = true, named = "carto-resolve-ops", priority = 9, threads = 16 )
    private ExecutorService executor;

    protected ResolveOps()
    {
    }

    public ResolveOps( final CalculationOps calculations, final DiscoverySourceManager sourceManager,
                       final ProjectRelationshipDiscoverer discoverer, final GraphAggregator aggregator,
                       final ArtifactManager artifacts, final ExecutorService executor,
                       final RelationshipGraphFactory graphFactory, final DTOResolver dtoResolver )
    {
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregator = aggregator;
        this.artifacts = artifacts;
        this.executor = executor;
        this.graphFactory = graphFactory;
        this.dtoResolver = dtoResolver;
    }

    /**
     * Resolve any variable versions in the specified root GAVs, retrieve, and if configured, discover missing parts of the relationship
     * graph. Return the {@link ViewParams} instance resulting from configuration via the given {@link AggregationOptions} and the root GAVs with
     * potential root GAV differences due to resolution of variable versions.
     */
    public ViewParams resolve( final String workspaceId, final AggregationOptions options,
                               final ProjectVersionRef... roots )
        throws CartoDataException
    {
        return resolve( workspaceId, options, true, null, roots );
    }

    /**
     * Resolve any variable versions in the specified root GAVs, retrieve, and if configured, discover missing parts of the relationship
     * graph. Return the {@link ViewParams} instance resulting from configuration via the given {@link AggregationOptions} and the root GAVs with
     * potential root GAV differences due to resolution of variable versions. If autoClose parameter is false, then leave the graph open for 
     * subsequent reuse.
     *
     * @param injectedDepMgmt map of versions managed by injected BOMs, can be {@code null}
     */
    public ViewParams resolve( final String workspaceId, final AggregationOptions options, final boolean autoClose,
                               final Map<ProjectRef, ProjectVersionRef> injectedDepMgmt,
                               final ProjectVersionRef... roots )
        throws CartoDataException
    {
        //        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( options.getDiscoveryConfig() );
        config.setEnabled( true );

        final List<? extends Location> locations =
            dtoResolver.resolveDiscoveryLocations( config, options.getDiscoverySource() );

        final List<ProjectVersionRef> specifics = new ArrayList<ProjectVersionRef>();

        for ( final ProjectVersionRef root : roots )
        {
            ProjectVersionRef specific = discoverer.resolveSpecificVersion( root, config );
            if ( specific == null )
            {
                specific = root;
            }

            specifics.add( specific );
        }

        final ViewParams params = new ViewParams.Builder( workspaceId, specifics ).withFilter( options.getFilter() )
                                                                                  .withMutator( options.getMutator() )
                                                                                  .withSelections( injectedDepMgmt )
                                                                                  .build();

        sourceManager.activateWorkspaceSources( params, locations );

        RelationshipGraph graph = null;

        //            else if ( !specific.equals( root ) )
        //            {
        //                view.selectVersion( root, specific );
        //            }

        try
        {
            try
            {
                graph = graphFactory.open( params, true );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Cannot open graph: {}. Reason: {}", e, params, e.getMessage() );
            }

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

                    if ( !options.isDiscoveryEnabled() )
                    {
                        logger.info( "Resolving direct relationships for root: {}", root );
                        final DiscoveryResult result = discoverer.discoverRelationships( root, graph, config );
                        logger.info( "Result: {} relationships", ( result == null ? 0
                                        : result.getAcceptedRelationships()
                                                .size() ) );
                    }
                }
            }

            if ( options.isDiscoveryEnabled() )
            {
                logger.info( "Performing graph discovery for: {}", specifics );
                aggregator.connectIncomplete( graph, options );
            }
        }
        finally
        {
            if ( autoClose )
            {
                CartoGraphUtils.closeGraphQuietly( graph );
            }
        }

        return params;
    }

    public Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveRepositoryContents( final RepositoryContentRecipe recipe )
        throws CartoDataException
    {
        dtoResolver.resolve( recipe );

        if ( recipe == null || !recipe.isValid() )
        {
            throw new CartoDataException( "Repository content recipe is invalid: {}", recipe );
        }

        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation()
                                                                   .getUri() );
        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '{}'. Use the form: '{}' instead.",
                                          recipe.getSourceLocation(), sourceManager.getFormatHint() );
        }

        final Map<ProjectVersionRef, ProjectRefCollection> refMap = resolveReferenceMap( recipe, sourceUri );
        final List<RepoContentCollector> collectors = collectContent( refMap, recipe, sourceUri );

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> itemMap =
            new HashMap<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>>();
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
                                                       final RepositoryContentRecipe recipe, final URI sourceUri )
        throws CartoDataException
    {
        final Location location = recipe.getSourceLocation();
        final Set<Location> excluded = recipe.getExcludedSourceLocations();

        if ( excluded != null && excluded.contains( location ) )
        {
            // no sense in going through all the rest if everything is excluded...
            throw new CartoDataException( "RepositoryContentRecipe is insane! Source location is among those excluded!" );
        }

        int projectCounter = 1;
        final int projectSz = refMap.size();
        final List<RepoContentCollector> collectors = new ArrayList<RepoContentCollector>( projectSz );

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
     * Lists all paths leading from roots defined in recipe to target projects for the configured graph composition.
     *
     * @param recipe the graph recipe
     * @param targets the set of target projects
     * @return the list of paths, each path is a list of project relationships
     */
    public List<List<ProjectRelationship<ProjectVersionRef>>> resolvePaths( final RepositoryContentRecipe recipe,
                                                                            final Set<ProjectRef> targets )
        throws CartoDataException
    {
        dtoResolver.resolve( recipe );

        final List<List<ProjectRelationship<ProjectVersionRef>>> discoveredPaths = new ArrayList<>();

        final GraphComposition graphComposition = recipe.getGraphComposition();
        final Map<GraphDescription, ViewParams> paramMap = resolve( recipe );

        // FIXME: Graph calculation isn't honored here.
        // Solution is to run the calculation and verify that all nodes in each accepted path are in the 
        // calculation result.
        for ( final GraphDescription gd : graphComposition )
        {
            final ViewParams params = paramMap.get( gd );
            final ProjectRelationshipFilter filter = gd.filter();

            final PathsTraversal paths = new PathsTraversal( filter, targets );
            RelationshipGraph graph = null;
            try
            {
                graph = graphFactory.open( params, false );
                graph.traverse( paths, TraversalType.depth_first );
            }
            catch ( final RelationshipGraphException ex )
            {
                throw new CartoDataException( "Failed to open / traverse the graph (for paths operation): "
                    + ex.getMessage(), ex );
            }
            finally
            {
                CartoGraphUtils.closeGraphQuietly( graph );
            }

            for ( final List<ProjectRelationship<?>> discoveredPath : paths.getDiscoveredPaths() )
            {
                @SuppressWarnings( { "unchecked", "rawtypes" } )
                final List<ProjectRelationship<ProjectVersionRef>> typed = (List) discoveredPath;
                discoveredPaths.add( typed );
            }
        }

        return discoveredPaths;
    }

    /**
     * Discover the dependency graphs for the configured graph composition, and then traverse them to construct a mapping of GAV to set of references
     * that can be used to render various kinds of output. If the recipe contains injectedBOMs, then read the managed dependencies from these into
     * a mapping of GA -> GAV that we can pass into the {@link ViewParams} we'll eventually use to discover and traverse the graph.
     * <br/>
     * Returns null if {@link RepositoryContentRecipe#setSourceLocation(Location)} hasn't 
     * been called before this method is called.
     * <br/>
     * @throws {@link CartoDataException} if one or more of the recipe's injected BOMs cannot be resolved, if the recipe doesn't contain enough basic 
     * info to be used (See: {@link RepositoryContentRecipe#isValid()}), if the source {@link Location} hasn't been set on the recipe, or if an 
     * unexpected problem takes place during graph resolution or traversal.
     */
    private Map<ProjectVersionRef, ProjectRefCollection> resolveReferenceMap( final RepositoryContentRecipe recipe,
                                                                              final URI sourceUri )
        throws CartoDataException
    {
        logger.info( "Building repository for: {}", recipe );

        dtoResolver.resolve( recipe );

        recipe.normalize();
        if ( !recipe.isValid() )
        {
            throw new CartoDataException( "Invalid repository recipe: {}", recipe );
        }

        final GraphComposition graphs = recipe.getGraphComposition();
        final Map<GraphDescription, ViewParams> paramMap = resolve( recipe );

        final Map<ProjectVersionRef, ProjectRefCollection> refMap;
        if ( graphs.getCalculation() != null && graphs.size() > 1 )
        {
            logger.info( "Collecting project references in composition: {}", recipe.getWorkspaceId() );
            final List<GraphDescription> realGraphs = new ArrayList<>();
            for ( final GraphDescription desc : graphs )
            {
                final ViewParams params = paramMap.get( desc );
                if ( params == null )
                {
                    continue;
                }

                realGraphs.add( new GraphDescription( params.getFilter(), params.getRoots() ) );
            }

            final GraphComposition realComp = new GraphComposition( graphs.getCalculation(), realGraphs );
            final GraphCalculation result = calculations.calculate( realComp, recipe.getWorkspaceId() );

            refMap = collectProjectVersionReferences( result.getResult() );
        }
        else
        {
            final GraphDescription graphDesc = graphs.getGraphs()
                                                     .get( 0 );

            final ViewParams params = paramMap.get( graphDesc );

            RelationshipGraph graph = null;
            try
            {
                graph = graphFactory.open( params, true );

                logger.info( "Collecting project references in single graph: {}", params.getWorkspaceId() );

                refMap = collectProjectVersionReferences( graph );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Cannot open graph: {}. Reason: {}", e, params, e.getMessage() );
            }
            finally
            {
                CartoGraphUtils.closeGraphQuietly( graph );
            }
        }

        for ( final GraphDescription desc : graphs )
        {
            for ( final ProjectVersionRef root : desc.getRoots() )
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

        return refMap;
    }

    /**
     * Resolve any variable versions in the specified root GAVs of the {@link GraphComposition} embeded in the 
     * {@link AbstractResolverRecipe}. Then retrieve, and if configured, discover missing parts of the relationship
     * graph. Return a mapping of {@link GraphDescription} to the {@link ViewParams} used during resolution, 
     * which might contain root GAVs different than those given in the original {@link AbstractResolverRecipe} due to
     * potential root GAV differences from resolution of variable versions.
     *
     * @param injectedDepMgmt map of versions managed by injected BOMs, can be {@code null}
     */
    public LinkedHashMap<GraphDescription, ViewParams> resolve( final AbstractResolverRecipe recipe )
        throws CartoDataException
    {
        dtoResolver.resolve( recipe );

        logger.info( "Initial source location: '{}'", recipe.getSourceLocation() );
        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation()
                                                                   .getUri() );

        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '{}'. Use the form: '{}' instead.",
                                          recipe.getSourceLocation(), sourceManager.getFormatHint() );
        }

        final AggregationOptions options = createAggregationOptions( recipe, sourceUri );

        final LinkedHashMap<GraphDescription, ViewParams> result = new LinkedHashMap<GraphDescription, ViewParams>();
        for ( final GraphDescription desc : recipe.getGraphComposition() )
        {
            final ProjectVersionRef[] rootsArray = desc.rootsArray();

            final ViewParams params;
            if ( recipe.isResolve() )
            {
                params =
                    resolve( recipe.getWorkspaceId(),
                             new DefaultAggregatorOptions( options, recipe.buildFilter( desc.filter() ) ), false,
                             recipe.getVersionSelections(), rootsArray );
            }
            else
            {
                params =
                    new ViewParams.Builder( recipe.getWorkspaceId(), rootsArray ).withFilter( desc.filter() )
                                                                                 .withMutator( new ManagedDependencyMutator() )
                                                                                 .withSelections( recipe.getVersionSelections() )
                                                                                 .build();
            }

            result.put( desc, params );
        }

        return result;
    }

    private AggregationOptions createAggregationOptions( final AbstractResolverRecipe recipe, final URI sourceUri )
        throws CartoDataException
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();

        options.setDiscoveryConfig( recipe.getDiscoveryConfig() );

        options.setProcessIncompleteSubgraphs( true );
        options.setProcessVariableSubgraphs( true );

        return options;
    }

}
