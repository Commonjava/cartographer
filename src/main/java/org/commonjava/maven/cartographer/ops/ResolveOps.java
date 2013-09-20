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
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
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

    protected ResolveOps()
    {
    }

    public ResolveOps( final CartoDataManager data, final DiscoverySourceManager sourceManager, final ProjectRelationshipDiscoverer discoverer,
                       final GraphAggregator aggregator, final ArtifactManager artifacts )
    {
        this.data = data;
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregator = aggregator;
        this.artifacts = artifacts;
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
        for ( final ProjectRefCollection refs : refMap.values() )
        {
            int artifactCounter = 1;
            final Set<ArtifactRef> artifactRefs = refs.getArtifactRefs();

            final int artifactSz = artifactRefs.size();
            for ( ArtifactRef ar : artifactRefs )
            {
                final Map<ArtifactRef, ConcreteResource> items = new HashMap<>();

                logger.info( "%d/%d %d/%d. Including: %s", projectCounter, projectSz, artifactCounter, artifactSz, ar );

                if ( ar.isVariableVersion() )
                {
                    final ProjectVersionRef specific = discoverer.resolveSpecificVersion( ar, options.getDiscoveryConfig() );
                    if ( specific == null )
                    {
                        // TODO: Is this really the best we can do??
                        continue;
                    }

                    ar =
                        new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), specific.getVersionSpec(), ar.getType(), ar.getClassifier(),
                                         ar.isOptional() );
                }

                logger.info( "%d/%d %d/%d 1. Resolving referenced artifact: %s", projectCounter, projectSz, artifactCounter, artifactSz, ar );
                final ConcreteResource mainArtifact = addToContent( ar, items, location, excluded, seen );
                if ( mainArtifact == null )
                {
                    logger.info( "Referenced artifact %s was excluded or not resolved. Skip trying pom and type/classifier extras.", ar );
                    continue;
                }

                // if multi-source GAVs are enabled, use the main location still
                // otherwise, restrict results for this GAV to the place where the main artifact came from.
                final Location artifactLocation = recipe.isMultiSourceGAVs() ? location : mainArtifact.getLocation();

                if ( !"pom".equals( ar.getType() ) )
                {
                    final ArtifactRef pomAR = new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), "pom", null, false );

                    logger.info( "%d/%d %d/%d 2. Resolving POM: %s", projectCounter, projectSz, artifactCounter, artifactSz, pomAR );
                    addToContent( pomAR, items, artifactLocation, excluded, seen );
                }
                else
                {
                    logger.info( "Referenced artifact: %s WAS a POM. Skipping special POM resolution.", ar );
                }

                final Set<ExtraCT> extras = recipe.getExtras();
                int extCounter = 3;
                if ( extras != null )
                {
                    if ( recipe.hasWildcardExtras() )
                    {
                        // 1. scan for all classifier/type for the GAV
                        TypeAndClassifier[] tcs;
                        try
                        {
                            tcs = artifacts.listAvailableArtifacts( location, ar.asProjectVersionRef() );
                        }
                        catch ( final TransferException e )
                        {
                            throw new CartoDataException( "Failed to list available type-classifier combinations for: %s from: %s. Reason: %s", e,
                                                          ar, location, e.getMessage() );
                        }

                        // 2. match up the resulting list against the extras we have
                        for ( final TypeAndClassifier tc : tcs )
                        {
                            for ( final ExtraCT extra : extras )
                            {
                                if ( extra == null )
                                {
                                    continue;
                                }

                                if ( extra.matches( tc ) )
                                {
                                    final ArtifactRef extAR = new ArtifactRef( ar, tc, false );
                                    logger.info( "%d/%d %d/%d %d/%d. Attempting to resolve classifier/type artifact from listing: %s",
                                                 projectCounter, projectSz, artifactCounter, artifactSz, extCounter, tcs.length, extAR );
                                    addToContent( extAR, items, artifactLocation, excluded, seen );
                                    break;
                                }
                            }

                            extCounter++;
                        }
                    }
                    else
                    {
                        for ( final ExtraCT extraCT : extras )
                        {
                            if ( extraCT == null )
                            {
                                continue;
                            }

                            final ArtifactRef extAR =
                                new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), extraCT.getType(),
                                                 extraCT.getClassifier(), false );

                            logger.info( "%d/%d %d/%d %d/%d. Attempting to resolve specifically listed classifier/type artifact: %s", projectCounter,
                                         projectSz, artifactCounter, artifactSz, extCounter, extras.size(), extAR );
                            addToContent( extAR, items, artifactLocation, excluded, seen );
                            extCounter++;
                        }
                    }
                }

                final Set<String> metas = recipe.getMetas();
                if ( metas != null && !metas.isEmpty() )
                {
                    logger.info( "Attempting to resolve metadata files for: %s", metas );

                    int metaCounter = extCounter;
                    final int metaSz = ( items.size() * metas.size() ) + extCounter;
                    for ( final Entry<ArtifactRef, ConcreteResource> entry : new HashMap<>( items ).entrySet() )
                    {
                        final ArtifactRef ref = entry.getKey();

                        // Let's see if we can skip iterating through the meta-type extensions
                        final String type = ref.getType();
                        final int idx = type.lastIndexOf( '.' );
                        if ( idx > 0 )
                        {
                            final String last = type.substring( idx + 1 );
                            if ( metas != null && metas.contains( last ) )
                            {
                                continue;
                            }
                        }

                        for ( final String meta : metas )
                        {
                            if ( meta == null )
                            {
                                continue;
                            }

                            if ( ref.getType()
                                    .endsWith( meta ) )
                            {
                                continue;
                            }

                            final ArtifactRef metaAR = ref.asArtifactRef( ref.getType() + "." + meta, ref.getClassifier() );

                            logger.info( "%d/%d %d/%d %d/%d. Attempting to resolve 'meta' artifact: %s", projectCounter, projectSz, artifactCounter,
                                         artifactSz, metaCounter, metaSz, metaAR );
                            addToContent( metaAR, items, artifactLocation, excluded, seen );
                            metaCounter++;
                        }
                    }
                }

                if ( !items.isEmpty() )
                {
                    itemMap.put( ar.asProjectVersionRef(), items );
                }

                artifactCounter++;
            }

            projectCounter++;
        }

        return itemMap;
    }

    private ConcreteResource addToContent( final ArtifactRef ar, final Map<ArtifactRef, ConcreteResource> items, final Location location,
                                           final Set<Location> excluded, final Set<ArtifactRef> seen )
        throws CartoDataException
    {
        if ( !seen.contains( ar ) )
        {
            seen.add( ar );

            final ConcreteResource item = resolve( ar, location, excluded, seen );
            if ( item != null )
            {
                logger.info( "+ %s", ar );
                items.put( ar, item );
            }
            else
            {
                logger.info( "- %s", ar );
            }

            return item;
        }
        else
        {
            logger.info( "- %s (ALREADY SEEN)", ar );
        }

        return null;
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

    private ConcreteResource resolve( final ArtifactRef ar, final Location location, final Set<Location> excluded, final Set<ArtifactRef> seen )
        throws CartoDataException
    {
        logger.info( "Attempting to resolve: %s from: %s", ar, location );
        ConcreteResource item;
        try
        {
            item = artifacts.checkExistence( location, ar );
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
