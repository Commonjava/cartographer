package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectReferences;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import org.commonjava.maven.cartographer.dto.ExtraCT;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.cartographer.preset.WorkspaceRecorder;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
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

    protected ResolveOps()
    {
    }

    public ResolveOps( final CartoDataManager data, final DiscoverySourceManager sourceManager,
                       final ProjectRelationshipDiscoverer discoverer, final GraphAggregator aggregator )
    {
        this.data = data;
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregator = aggregator;
    }

    public List<ProjectVersionRef> resolve( final String fromUri, final AggregationOptions options,
                                            final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final URI source = sourceManager.createSourceURI( fromUri );
        if ( source == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.", fromUri,
                                          sourceManager.getFormatHint() );
        }

        GraphWorkspace ws = data.getCurrentWorkspace();
        if ( ws == null )
        {
            ws = data.createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
        }

        sourceManager.activateWorkspaceSources( data.getCurrentWorkspace(), fromUri );

        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        final List<ProjectVersionRef> results = new ArrayList<>();
        for ( final ProjectVersionRef root : roots )
        {
            final ProjectVersionRef specific = discoverer.resolveSpecificVersion( root, config );
            if ( !data.contains( specific ) )
            {
                final DiscoveryResult result = discoverer.discoverRelationships( specific, config );
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

    public Map<ProjectVersionRef, Map<ArtifactRef, Transfer>> resolveRepositoryContents( final RepositoryContentRecipe recipe )
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
                                                                   .toString() );
        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.",
                                          recipe.getSourceLocation(), sourceManager.getFormatHint() );
        }

        logger.info( "Building repository for: %s", recipe );

        EProjectWeb web = null;
        data.setCurrentWorkspace( recipe.getWorkspaceId() );

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
            return null;
        }

        final Map<ProjectRef, ProjectRefCollection> refMap = collectProjectReferences( web );

        final Set<ExtraCT> extras = recipe.getExtras();
        final Set<String> metas = recipe.getMetas();

        final Set<ArtifactRef> seen = new HashSet<>();
        final Location location = recipe.getSourceLocation();
        final Set<Location> excluded = recipe.getExcludedSourceLocations();

        if ( excluded != null && excluded.contains( location ) )
        {
            // no sense in going through all the rest if everything is excluded...
            throw new CartoDataException( "RepositoryContentRecipe is insane! Source location is among those excluded!" );
        }

        final Map<ProjectVersionRef, Map<ArtifactRef, Transfer>> itemMap = new HashMap<>();
        for ( final ProjectRefCollection refs : refMap.values() )
        {
            for ( ArtifactRef ar : refs.getArtifactRefs() )
            {
                final Map<ArtifactRef, Transfer> items = new HashMap<>();
                itemMap.put( ar.asProjectVersionRef(), items );

                logger.info( "Including: %s", ar );

                if ( ar.isVariableVersion() )
                {
                    final ProjectVersionRef specific =
                        discoverer.resolveSpecificVersion( ar, options.getDiscoveryConfig() );
                    ar =
                        new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), specific.getVersionSpec(), ar.getType(),
                                         ar.getClassifier(), ar.isOptional() );
                }

                if ( !"pom".equals( ar.getType() ) )
                {
                    final ArtifactRef pomAR =
                        new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), "pom", null, false );

                    if ( !seen.contains( pomAR ) )
                    {
                        final Transfer item = resolve( pomAR, location, excluded );
                        if ( item != null )
                        {
                            logger.info( "+ %s", pomAR );
                            items.put( pomAR, item );
                        }
                        else
                        {
                            logger.info( "- %s", pomAR );
                        }
                        seen.add( pomAR );
                    }
                }

                if ( !seen.contains( ar ) )
                {
                    final Transfer item = resolve( ar, location, excluded );
                    if ( item != null )
                    {
                        logger.info( "+ %s", ar );
                        items.put( ar, item );
                    }
                    else
                    {
                        logger.info( "- %s", ar );
                    }
                    seen.add( ar );
                }

                if ( extras != null )
                {
                    for ( final ExtraCT extraCT : extras )
                    {
                        if ( extraCT == null )
                        {
                            continue;
                        }

                        final ArtifactRef extAR =
                            new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(),
                                             extraCT.getType(), extraCT.getClassifier(), false );

                        if ( !seen.contains( extAR ) )
                        {
                            final Transfer item = resolve( extAR, location, excluded );
                            if ( item != null )
                            {
                                logger.info( "+ %s", extAR );
                                items.put( extAR, item );
                            }
                            else
                            {
                                logger.info( "- %s", extAR );
                            }

                            seen.add( extAR );
                        }
                    }
                }

                if ( metas != null && !metas.isEmpty() )
                {
                    for ( final Entry<ArtifactRef, Transfer> entry : new HashMap<>( items ).entrySet() )
                    {
                        final ArtifactRef ref = entry.getKey();

                        for ( final String meta : metas )
                        {
                            if ( meta == null )
                            {
                                continue;
                            }

                            final ArtifactRef metaAR =
                                ref.asArtifactRef( ref.getType() + "." + meta, ref.getClassifier() );

                            final Transfer metaItem = resolve( metaAR, location, excluded );
                            if ( metaItem != null )
                            {
                                logger.info( "+ %s", metaAR );
                                items.put( metaAR, metaItem );
                            }
                            else
                            {
                                logger.info( "- %s", metaAR );
                            }
                        }
                    }
                }
            }
        }

        return itemMap;
    }

    private AggregationOptions createAggregationOptions( final RepositoryContentRecipe recipe, final URI sourceUri )
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();
        options.setFilter( recipe.getFilter() );

        final DefaultDiscoveryConfig dconf = new DefaultDiscoveryConfig( sourceUri );

        dconf.setEnabled( true );
        dconf.setTimeoutMillis( 1000 * recipe.getTimeoutSecs() );

        options.setDiscoveryConfig( dconf );

        options.setProcessIncompleteSubgraphs( true );
        options.setProcessVariableSubgraphs( true );

        return options;
    }

    private Transfer resolve( final ArtifactRef ar, final Location location, final Set<Location> excluded )
        throws CartoDataException
    {
        final String version = ar.getVersionString();

        final StringBuilder sb = new StringBuilder();
        sb.append( ar.getArtifactId() )
          .append( '-' )
          .append( version );
        if ( ar.getClassifier() != null )
        {
            sb.append( '-' )
              .append( ar.getClassifier() );
        }

        sb.append( '.' );
        if ( "maven-plugin".equals( ar.getType() ) )
        {
            sb.append( "jar" );
        }
        else
        {
            sb.append( ar.getType() );
        }

        logger.info( "Attempting to resolve: %s from: %s", ar, location );
        Transfer item;
        try
        {
            item = artifacts.retrieve( location, ar );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve: %s from: %s. Reason: %s", e, ar, location, e.getMessage() );
        }

        logger.info( "Got: %s", item );

        if ( item == null )
        {
            logger.warn( "NOT FOUND: %s", ar );
        }
        else if ( excluded != null && excluded.contains( item.getLocation() ) )
        {
            logger.info( "EXCLUDED: %s (Location was: %s)", ar, item.getLocation() );
            return null;
        }

        return item;
    }
}
