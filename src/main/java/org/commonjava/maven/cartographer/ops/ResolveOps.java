package org.commonjava.maven.cartographer.ops;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectReferences;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.cartographer.preset.WorkspaceRecorder;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.type.TypeMapper;
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
    private TypeMapper typeMapper;

    @Inject
    @ExecutorConfig( daemon = true, named = "carto-resolve-ops", priority = 9, threads = 100 )
    private ExecutorService executor;

    protected ResolveOps()
    {
    }

    public ResolveOps( final CartoDataManager data, final DiscoverySourceManager sourceManager, final ProjectRelationshipDiscoverer discoverer,
                       final GraphAggregator aggregator, final ArtifactManager artifacts, final ExecutorService executor )
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
        if ( recipe == null )
        {
            throw new CartoDataException( "Repository content recipe is missing." );
        }

        if ( !recipe.isValid() )
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

        logger.info( "Building repository for: %s", recipe );

        EProjectWeb web = null;

        if ( data.getCurrentWorkspace() == null || !recipe.getWorkspaceId()
                                                          .equals( data.getCurrentWorkspace()
                                                                       .getId() ) )
        {
            data.setCurrentWorkspace( recipe.getWorkspaceId() );
        }

        sourceManager.activateWorkspaceSources( data.getCurrentWorkspace(), sourceUri.toString() );

        Collection<ProjectVersionRef> roots = recipe.getRoots();
        for ( final Iterator<ProjectVersionRef> it = roots.iterator(); it.hasNext(); )
        {
            if ( it.next() == null )
            {
                it.remove();
            }
        }

        ProjectVersionRef[] rootsArray = roots.toArray( new ProjectVersionRef[roots.size()] );

        final AggregationOptions options = createAggregationOptions( recipe, sourceUri );
        if ( recipe.isResolve() )
        {
            roots = resolve( recipe.getSourceLocation()
                                   .toString(), options, rootsArray );

            rootsArray = roots.toArray( new ProjectVersionRef[roots.size()] );
        }

        web = data.getProjectWeb( recipe.getFilter(), rootsArray );
        if ( web == null )
        {
            throw new CartoDataException( "Failed to retrieve web for roots: %s (attempted resolve? %s)", roots, recipe.isResolve() );
        }

        final Map<ProjectRef, ProjectRefCollection> refMap = collectProjectReferences( web );

        final Set<ArtifactRef> seen = new HashSet<>();
        final Location location = recipe.getSourceLocation();
        final Set<Location> excluded = recipe.getExcludedSourceLocations();

        if ( excluded != null && excluded.contains( location ) )
        {
            // no sense in going through all the rest if everything is excluded...
            throw new CartoDataException( "RepositoryContentRecipe is insane! Source location is among those excluded!" );
        }

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> itemMap = new HashMap<>();
        int projectCounter = 1;
        final int projectSz = refMap.size();
        final List<RepoContentCollector> collectors = new ArrayList<>( projectSz );
        for ( final ProjectRefCollection refs : refMap.values() )
        {
            int artifactCounter = 1;
            final Set<ArtifactRef> artifactRefs = refs.getArtifactRefs();

            final int artifactSz = artifactRefs.size();
            for ( final ArtifactRef ar : artifactRefs )
            {
                final RepoContentCollector collector =
                    new RepoContentCollector( ar, recipe, location, options, artifacts, discoverer, typeMapper, excluded, seen, projectCounter,
                                              projectSz, artifactCounter++, artifactSz );

                collectors.add( collector );
            }

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

        for ( final RepoContentCollector collector : collectors )
        {
            final Map<ArtifactRef, ConcreteResource> items = collector.getItems();

            if ( items != null && !items.isEmpty() )
            {
                logger.info( "Returning for: %s\n\n  %s", collector.getRef(), join( items.entrySet(), "\n  " ) );
                itemMap.put( collector.getRef(), items );
            }
            else
            {
                logger.warn( "No items returned for: %s", collector.getRef() );
            }
        }

        return itemMap;
    }

    private AggregationOptions createAggregationOptions( final RepositoryContentRecipe recipe, final URI sourceUri )
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();
        options.setFilter( recipe.getFilter() );

        final DefaultDiscoveryConfig dconf = new DefaultDiscoveryConfig( sourceUri );
        dconf.setEnabledPatchers( recipe.getPatcherIds() );

        dconf.setEnabled( true );
        dconf.setTimeoutMillis( 1000 * recipe.getTimeoutSecs() );

        options.setDiscoveryConfig( dconf );

        options.setProcessIncompleteSubgraphs( true );
        options.setProcessVariableSubgraphs( true );

        return options;
    }

}
