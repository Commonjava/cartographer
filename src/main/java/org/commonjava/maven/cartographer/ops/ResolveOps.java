/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectVersionReferences;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
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
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.cartographer.dto.ResolverRecipe;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
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
    @ExecutorConfig( daemon = true, named = "carto-resolve-ops", priority = 9, threads = 16 )
    private ExecutorService executor;

    private final Map<ProjectVersionRef, MavenPomView> bomMap = new WeakHashMap<ProjectVersionRef, MavenPomView>();

    protected ResolveOps()
    {
    }

    public ResolveOps( final CalculationOps calculations, final DiscoverySourceManager sourceManager,
                       final ProjectRelationshipDiscoverer discoverer, final GraphAggregator aggregator,
                       final ArtifactManager artifacts, final ExecutorService executor,
                       final RelationshipGraphFactory graphFactory )
    {
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregator = aggregator;
        this.artifacts = artifacts;
        this.executor = executor;
        this.graphFactory = graphFactory;
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
        return resolve( workspaceId, options, true, roots );
    }

    /**
     * Resolve any variable versions in the specified root GAVs, retrieve, and if configured, discover missing parts of the relationship
     * graph. Return the {@link ViewParams} instance resulting from configuration via the given {@link AggregationOptions} and the root GAVs with
     * potential root GAV differences due to resolution of variable versions. If autoClose parameter is false, then leave the graph open for 
     * subsequent reuse.
     */
    public ViewParams resolve( final String workspaceId, final AggregationOptions options,
                               final boolean autoClose,
                               final ProjectVersionRef... roots )
        throws CartoDataException
    {
        //        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( options.getDiscoveryConfig() );
        config.setEnabled( true );

        final List<? extends Location> locations = initDiscoveryLocations( config );
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

    private MavenPomView getBOM( final ProjectVersionRef projectVersionRef, final List<? extends Location> locations )
        throws GalleyMavenException
    {
        MavenPomView result = bomMap.get( projectVersionRef );

        if ( result == null )
        {
            result = pomReader.read( projectVersionRef, locations );
            bomMap.put( projectVersionRef, result );
        }

        return result;
    }

    /**
     * Create one or more {@link Location} instances for the configured discovery source, according to the {@link DiscoverySourceManager} 
     * implementation's specific logic, if it hasn't already been done.
     */
    private List<? extends Location> initDiscoveryLocations( final DiscoveryConfig config )
    {
        List<? extends Location> locations = config.getLocations();
        if ( locations == null || locations.isEmpty() )
        {
            locations = sourceManager.createLocations( config.getDiscoverySource() );
            config.setLocations( locations );
        }

        return locations;
    }

    public Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveRepositoryContents( final RepositoryContentRecipe recipe )
        throws CartoDataException
    {
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

        final DiscoveryConfig dconf = createDiscoveryConfig( recipe, sourceUri );

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
        try
        {
            latch.await();
        }
        catch ( final InterruptedException e )
        {
            logger.error( "Abandoning repo-content assembly for: {}", recipe );
        }

        return collectors;
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

        recipe.normalize();
        if ( !recipe.isValid() )
        {
            throw new CartoDataException( "Invalid repository recipe: {}", recipe );
        }

        GraphComposition graphs = recipe.getGraphComposition();
        final boolean hasCalculation = graphs.getCalculation() != null && graphs.size() > 1;

        DiscoveryConfig config;
        try
        {
            config = recipe.getDiscoveryConfig();
        }
        catch ( final URISyntaxException e )
        {
            throw new CartoDataException( "Invalid discovery source URI: '{}'. Reason: {}", e,
                                          recipe.getSourceLocation()
                                                .getUri(), e.getMessage() );
        }

        final List<? extends Location> locations = initDiscoveryLocations( config );

        final Map<VersionlessArtifactRef, String> injectedDepMgmt = new HashMap<VersionlessArtifactRef, String>();
        if ( recipe.getInjectedBOMs() != null )
        {
            // TODO read depMgmt recursively to include anything imported in the referenced BOMs
            for ( final ProjectVersionRef bom : recipe.getInjectedBOMs() )
            {
                try
                {
                    final MavenPomView bomView = getBOM( bom, locations );
                    final List<DependencyView> managedDependencies = bomView.getAllManagedDependencies();
                    for ( final DependencyView managedDependency : managedDependencies )
                    {
                        final VersionlessArtifactRef ar = managedDependency.asVersionlessArtifactRef();
                        final String version = managedDependency.getVersion();
                        if ( !injectedDepMgmt.containsKey( ar ) )
                        {
                            injectedDepMgmt.put( ar, version );
                        }
                    }
                }
                catch ( final GalleyMavenException ex )
                {
                    logger.error( "BOM " + bom.toString() + " not available." , ex);
                }
            }
        }

        if ( recipe.isResolve() )
        {
            // TODO pass the injected depMgmt
            graphs = resolve( recipe, !hasCalculation );
        }

        final Map<ProjectVersionRef, ProjectRefCollection> refMap;
        RelationshipGraph graph = null;
        try
        {
            if ( graphs.getCalculation() != null && graphs.size() > 1 )
            {

                logger.info( "Collecting project references in composition: {}", recipe.getWorkspaceId() );
                final GraphCalculation result = calculations.calculate( graphs, recipe.getWorkspaceId() );

                refMap = collectProjectVersionReferences( result.getResult() );
            }
            else
            {
                final GraphDescription graphDesc = graphs.getGraphs()
                                                         .get( 0 );

                final ProjectVersionRef[] roots = graphDesc.getRootsArray();

                // TODO: Do we need to resolve specific versions for these roots?
                final ViewParams params =
                    new ViewParams.Builder( recipe.getWorkspaceId(), roots ).withFilter( graphDesc.getFilter() )
                                                                            .withMutator( new ManagedDependencyMutator() )
                                                                            .build();

                sourceManager.activateWorkspaceSources( params, locations );

                try
                {
                    graph = graphFactory.open( params, true );
                }
                catch ( final RelationshipGraphException e )
                {
                    throw new CartoDataException( "Cannot open graph: {}. Reason: {}", e, params, e.getMessage() );
                }

                logger.info( "Collecting project references in single graph: {}", params.getWorkspaceId() );

                refMap = collectProjectVersionReferences( graph );
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
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }

        return refMap;
    }

    public GraphComposition resolve( final ResolverRecipe recipe )
        throws CartoDataException
    {
        return resolve( recipe, true );
    }

    /**
     * Resolve any variable versions in the specified root GAVs of the {@link GraphComposition} embeded in the {@link ResolverRecipe}. Then retrieve, 
     * and if configured, discover missing parts of the relationship
     * graph. Return the {@link GraphComposition} instance, which might be different than the one in the given {@link ResolverRecipe} due to
     * potential root GAV differences from resolution of variable versions.
     */
    public GraphComposition resolve( final ResolverRecipe recipe, final boolean autoClose )
        throws CartoDataException
    {
        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation()
                                                                   .getUri() );
        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '{}'. Use the form: '{}' instead.",
                                          recipe.getSourceLocation(), sourceManager.getFormatHint() );
        }

        final List<GraphDescription> outDescs = new ArrayList<GraphDescription>( recipe.getGraphComposition()
                                                                                       .size() );

        final AggregationOptions options = createAggregationOptions( recipe, sourceUri );

        for ( final GraphDescription desc : recipe.getGraphComposition() )
        {
            final ProjectVersionRef[] rootsArray = desc.getRootsArray();

            final ViewParams params =
                resolve( recipe.getWorkspaceId(), new DefaultAggregatorOptions( options, desc.getFilter() ), false,
                         rootsArray );

            RelationshipGraph graph = null;
            try
            {
                try
                {
                    graph = graphFactory.open( params, false );
                }
                catch ( final RelationshipGraphException e )
                {
                    throw new CartoDataException(
                                                  "Cannot re-open graph that was created during resolve step! Params: {}\nError: {}\nRecipe: {}",
                                                  e, params, e.getMessage(), recipe );
                }
                if ( graph.getRoots()
                          .isEmpty() )
                {
                    // best guess if the roots came back empty...
                    outDescs.add( desc );
                }
                else
                {
                    final GraphDescription outGraph = new GraphDescription( graph.getFilter(), rootsArray );
                    outGraph.setGraphParams( graph.getParams() );

                    outDescs.add( outGraph );
                }
            }
            finally
            {
                if ( autoClose )
                {
                    CartoGraphUtils.closeGraphQuietly( graph );
                }
            }
        }

        return new GraphComposition( recipe.getGraphComposition()
                                           .getCalculation(), outDescs );
    }

    private AggregationOptions createAggregationOptions( final ResolverRecipe recipe, final URI sourceUri )
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();

        options.setDiscoveryConfig( createDiscoveryConfig( recipe, sourceUri ) );

        options.setProcessIncompleteSubgraphs( true );
        options.setProcessVariableSubgraphs( true );

        return options;
    }

    private DiscoveryConfig createDiscoveryConfig( final ResolverRecipe recipe, final URI sourceUri )
    {
        final DefaultDiscoveryConfig dconf = new DefaultDiscoveryConfig( sourceUri );
        dconf.setEnabledPatchers( recipe.getPatcherIds() );

        dconf.setEnabled( recipe.isResolve() );
        dconf.setTimeoutMillis( 1000 * recipe.getTimeoutSecs() );

        initDiscoveryLocations( dconf );

        return dconf;
    }

}
