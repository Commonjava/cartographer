package org.commonjava.maven.cartographer.ops;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.dto.ExtraCT;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.type.TypeMapper;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.util.logging.Logger;

public class RepoContentCollector
    implements Runnable
{

    private final Logger logger = new Logger( getClass() );

    private final int projectCounter;

    private final int projectSz;

    private CountDownLatch latch;

    private final AggregationOptions options;

    private final ProjectRelationshipDiscoverer discoverer;

    private final RepositoryContentRecipe recipe;

    private final Set<ArtifactRef> seen;

    private final Set<Location> excluded;

    private final Location location;

    private final ArtifactManager artifacts;

    private final TypeMapper typeMapper;

    private ArtifactRef ar;

    private final int artifactCounter;

    private final int artifactSz;

    private Map<ArtifactRef, ConcreteResource> items;

    private CartoDataException error;

    public RepoContentCollector( final ArtifactRef ar, final RepositoryContentRecipe recipe, final Location location,
                                 final AggregationOptions options, final ArtifactManager artifacts, final ProjectRelationshipDiscoverer discoverer,
                                 final TypeMapper typeMapper, final Set<Location> excluded, final Set<ArtifactRef> seen, final int projectCounter,
                                 final int projectSz, final int artifactCounter, final int artifactSz )
    {
        this.ar = ar;
        this.recipe = recipe;
        this.location = location;
        this.options = options;
        this.artifacts = artifacts;
        this.discoverer = discoverer;
        this.typeMapper = typeMapper;
        this.excluded = excluded;
        this.seen = seen;
        this.projectCounter = projectCounter;
        this.projectSz = projectSz;
        this.artifactCounter = artifactCounter;
        this.artifactSz = artifactSz;
    }

    @Override
    public void run()
    {
        try
        {
            execute();
        }
        catch ( final CartoDataException e )
        {
            this.error = e;
        }
    }

    private void execute()
        throws CartoDataException
    {
        try
        {
            final Map<ArtifactRef, ConcreteResource> items = new HashMap<>();

            logger.info( "%d/%d %d/%d. Including: %s", projectCounter, projectSz, artifactCounter, artifactSz, ar );

            if ( ar.isVariableVersion() )
            {
                final ProjectVersionRef specific = discoverer.resolveSpecificVersion( ar, options.getDiscoveryConfig() );
                if ( specific == null )
                {
                    logger.error( "No version available for variable reference: %s. Skipping.", ar.asProjectVersionRef() );
                    return;
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
                return;
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
                        throw new CartoDataException( "Failed to list available type-classifier combinations for: %s from: %s. Reason: %s", e, ar,
                                                      location, e.getMessage() );
                    }

                    // 2. match up the resulting list against the extras we have
                    tsAndCs: for ( final TypeAndClassifier tc : tcs )
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
                                logger.info( "%d/%d %d/%d %d/%d. Attempting to resolve classifier/type artifact from listing: %s", projectCounter,
                                             projectSz, artifactCounter, artifactSz, extCounter, tcs.length, extAR );

                                // if we're using a listing for wildcards, we've already established that these exist...
                                // so don't waste the time on individual calls!
                                //
                                // addToContent( extAR, items, artifactLocation, excluded, seen );
                                //
                                ConcreteResource item;
                                try
                                {
                                    item = new ConcreteResource( location, PathUtils.formatArtifactPath( extAR, typeMapper ) );
                                }
                                catch ( final TransferException e )
                                {
                                    logger.error( "SHOULD NEVER HAPPEN: %s", e, e.getMessage() );
                                    break tsAndCs;
                                }

                                items.put( ar, item );
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
                            new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), extraCT.getType(), extraCT.getClassifier(),
                                             false );

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

            this.items = items;
        }
        finally
        {
            if ( latch != null )
            {
                latch.countDown();
            }
        }
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

    public void setLatch( final CountDownLatch latch )
    {
        this.latch = latch;
    }

    public ProjectVersionRef getRef()
    {
        return ar.asProjectVersionRef();
    }

    public Map<ArtifactRef, ConcreteResource> getItems()
    {
        return items;
    }

    public CartoDataException getError()
    {
        return error;
    }
}
