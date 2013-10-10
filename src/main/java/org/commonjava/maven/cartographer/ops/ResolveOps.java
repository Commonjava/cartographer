package org.commonjava.maven.cartographer.ops;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectVersionReferences;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
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
import org.commonjava.maven.cartographer.preset.WorkspaceRecorder;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class ResolveOps
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    private GraphAggregator aggregator;

    @Inject
    private CartoDataManager data;

    @Inject
    private ArtifactManager artifacts;

    @Inject
    private CalculationOps calculations;

    @Inject
    @ExecutorConfig( daemon = true, named = "carto-resolve-ops", priority = 9, threads = 100 )
    private ExecutorService executor;

    protected ResolveOps()
    {
    }

    public ResolveOps( final CalculationOps calculations, final CartoDataManager data, final DiscoverySourceManager sourceManager,
                       final ProjectRelationshipDiscoverer discoverer, final GraphAggregator aggregator, final ArtifactManager artifacts,
                       final ExecutorService executor )
    {
        this.data = data;
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregator = aggregator;
        this.artifacts = artifacts;
        this.executor = executor;
    }

    public List<ProjectVersionRef> resolve( final String fromUri, final AggregationOptions options, final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final URI source = sourceManager.createSourceURI( fromUri );
        if ( source == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.", fromUri, sourceManager.getFormatHint() );
        }

        GraphWorkspace ws = data.getCurrentWorkspace();
        if ( ws == null )
        {
            ws = data.createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
        }

        sourceManager.activateWorkspaceSources( data.getCurrentWorkspace(), fromUri );

        //        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( options.getDiscoveryConfig() );
        config.setEnabled( true );

        final List<ProjectVersionRef> results = new ArrayList<>();
        for ( final ProjectVersionRef root : roots )
        {
            final ProjectVersionRef specific = discoverer.resolveSpecificVersion( root, config );

            boolean doDiscovery = !data.contains( specific );
            if ( !doDiscovery && data.hasErrors( specific ) )
            {
                data.clearErrors( specific );
                doDiscovery = true;
            }

            if ( doDiscovery )
            {
                final DiscoveryResult result = discoverer.discoverRelationships( specific, config, true );
                if ( result != null && data.contains( result.getSelectedRef() ) )
                {
                    results.add( result.getSelectedRef() );
                }
            }
            else
            {
                results.add( specific );
            }
        }

        final ProjectRelationshipFilter filter = options.getFilter();
        final ProjectVersionRef[] resultsArray = results.toArray( new ProjectVersionRef[results.size()] );

        logger.info( "Retrieving web for roots: %s with filter: %s", resultsArray, filter );

        final EProjectWeb web = data.getProjectWeb( filter, resultsArray );
        if ( options.isDiscoveryEnabled() )
        {
            aggregator.connectIncomplete( web, options );
        }

        if ( filter != null && ( filter instanceof WorkspaceRecorder ) )
        {
            ( (WorkspaceRecorder) filter ).save( data.getCurrentWorkspace() );
        }

        return results;
    }

    public Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveRepositoryContents( final RepositoryContentRecipe recipe )
        throws CartoDataException
    {
        if ( recipe == null || !recipe.isValid() )
        {
            throw new CartoDataException( "Repository content recipe is invalid: %s", recipe );
        }

        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation()
                                                                   .getUri() );
        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.", recipe.getSourceLocation(),
                                          sourceManager.getFormatHint() );
        }

        final Map<ProjectVersionRef, ProjectRefCollection> refMap = resolveReferenceMap( recipe, sourceUri );
        final List<RepoContentCollector> collectors = collectContent( refMap, recipe, sourceUri );

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> itemMap = new HashMap<>();
        for ( final RepoContentCollector collector : collectors )
        {
            final Map<ArtifactRef, ConcreteResource> items = collector.getItems();

            if ( items != null && !items.isEmpty() )
            {
                logger.info( "%s Returning for: %s\n\n  %s", collector, collector.getRef(), join( items.entrySet(), "\n  " ) );
                itemMap.put( collector.getRef(), items );
            }
            else
            {
                logger.warn( "%s No items returned for: %s", collector, collector.getRef() );
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
        final List<RepoContentCollector> collectors = new ArrayList<>( projectSz );

        final DiscoveryConfig dconf = createDiscoveryConfig( recipe, sourceUri );

        for ( final Map.Entry<ProjectVersionRef, ProjectRefCollection> entry : refMap.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final ProjectRefCollection refs = entry.getValue();

            final RepoContentCollector collector =
                new RepoContentCollector( ref, refs, recipe, location, dconf, artifacts, discoverer, excluded, projectCounter, projectSz );

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
            logger.error( "Abandoning repo-content assembly for: %s", recipe );
        }

        return collectors;
    }

    private Map<ProjectVersionRef, ProjectRefCollection> resolveReferenceMap( final RepositoryContentRecipe recipe, final URI sourceUri )
        throws CartoDataException
    {
        logger.info( "Building repository for: %s", recipe );

        EProjectNet web = null;

        if ( data.getCurrentWorkspace() == null || !recipe.getWorkspaceId()
                                                          .equals( data.getCurrentWorkspace()
                                                                       .getId() ) )
        {
            data.setCurrentWorkspace( recipe.getWorkspaceId() );
        }

        sourceManager.activateWorkspaceSources( data.getCurrentWorkspace(), sourceUri.toString() );

        recipe.normalize();
        if ( !recipe.isValid() )
        {
            throw new CartoDataException( "Invalid repository recipe: %s", recipe );
        }

        final GraphComposition graphs = recipe.getGraphComposition();

        if ( recipe.isResolve() )
        {
            resolve( recipe );
        }

        final Map<ProjectVersionRef, ProjectRefCollection> refMap;
        if ( graphs.getCalculation() != null && graphs.size() > 1 )
        {
            final GraphCalculation result = calculations.calculate( graphs );
            refMap = collectProjectVersionReferences( result.getResult() );
        }
        else
        {
            final GraphDescription graphDesc = graphs.getGraphs()
                                                     .get( 0 );
            final ProjectVersionRef[] roots = graphDesc.getRootsArray();
            web = data.getProjectWeb( graphDesc.getFilter(), roots );

            if ( web == null )
            {
                throw new CartoDataException( "Failed to retrieve web for roots: %s", join( roots, ", " ) );
            }

            refMap = collectProjectVersionReferences( web );
        }

        return refMap;
    }

    public void resolve( final ResolverRecipe recipe )
        throws CartoDataException
    {
        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation()
                                                                   .getUri() );
        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.", recipe.getSourceLocation(),
                                          sourceManager.getFormatHint() );
        }

        for ( final GraphDescription graph : recipe.getGraphComposition() )
        {
            final AggregationOptions options = createAggregationOptions( recipe, graph.getFilter(), sourceUri );

            final ProjectVersionRef[] rootsArray = graph.getRootsArray();

            final List<ProjectVersionRef> roots = resolve( sourceUri.toString(), options, rootsArray );

            graph.setRoots( new HashSet<>( roots ) );
        }
    }

    private AggregationOptions createAggregationOptions( final ResolverRecipe recipe, final ProjectRelationshipFilter filter, final URI sourceUri )
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();
        options.setFilter( filter );

        options.setDiscoveryConfig( createDiscoveryConfig( recipe, sourceUri ) );

        options.setProcessIncompleteSubgraphs( true );
        options.setProcessVariableSubgraphs( true );

        return options;
    }

    private DiscoveryConfig createDiscoveryConfig( final ResolverRecipe recipe, final URI sourceUri )
    {
        final DefaultDiscoveryConfig dconf = new DefaultDiscoveryConfig( sourceUri );
        dconf.setEnabledPatchers( recipe.getPatcherIds() );

        dconf.setEnabled( true );
        dconf.setTimeoutMillis( 1000 * recipe.getTimeoutSecs() );

        return dconf;
    }

}
