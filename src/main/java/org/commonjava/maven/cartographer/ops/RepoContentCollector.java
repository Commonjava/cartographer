/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.cartographer.ops;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.commonjava.maven.atlas.ident.util.StringFormat;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.dto.ExtraCT;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoContentCollector
    implements Runnable
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final int projectCounter;

    private final int projectSz;

    private CountDownLatch latch;

    private final ProjectRelationshipDiscoverer discoverer;

    private final RepositoryContentRecipe recipe;

    private final Set<ArtifactRef> seen = new HashSet<ArtifactRef>();

    private final Set<Location> excluded;

    private final Location location;

    private final ArtifactManager artifacts;

    private int counter;

    private final int artifactSz;

    private final DiscoveryConfig discoveryConfig;

    private Map<ArtifactRef, ConcreteResource> items;

    private final Map<ArtifactRef, CartoDataException> errors = new HashMap<ArtifactRef, CartoDataException>();

    private String name;

    private String originalName;

    private final Set<ArtifactRef> refs;

    private final ProjectVersionRef ref;

    public RepoContentCollector( final ProjectVersionRef ref, final ProjectRefCollection refs, final RepositoryContentRecipe recipe,
                                 final Location location, final DiscoveryConfig discoveryConfig, final ArtifactManager artifacts,
                                 final ProjectRelationshipDiscoverer discoverer, final Set<Location> excluded, final int projectCounter,
                                 final int projectSz )
    {
        this.ref = ref;
        this.refs = refs.getArtifactRefs();
        this.recipe = recipe;
        this.location = location;
        this.discoveryConfig = discoveryConfig;
        this.artifacts = artifacts;
        this.discoverer = discoverer;
        this.excluded = excluded;
        this.projectCounter = projectCounter;
        this.projectSz = projectSz;
        this.counter = 0;
        this.artifactSz = refs.getArtifactRefs()
                              .size();
    }

    @Override
    public void run()
    {
        this.originalName = Thread.currentThread()
                                  .getName();
        try
        {
            Thread.currentThread()
                  .setName( originalName + ":" + ref );

            this.name = Thread.currentThread()
                              .getName();

            items = new HashMap<ArtifactRef, ConcreteResource>();

            for ( final ArtifactRef ar : refs )
            {
                if ( refs.size() > 1 && "pom".equals( ar.getType() ) )
                {
                    // handled later.
                    continue;
                }

                try
                {
                    execute( ar, counter++ );
                }
                catch ( final CartoDataException e )
                {
                    logger.error( "{}", e, new StringFormat( "ERROR for {}: {}", ar, e.getMessage() ) );
                    this.errors.put( ar, e );
                }
            }
        }
        finally
        {
            Thread.currentThread()
                  .setName( originalName );

            if ( latch != null )
            {
                latch.countDown();
            }
        }
    }

    private void execute( ArtifactRef ar, final int artifactCounter )
        throws CartoDataException
    {
        logger.info( "{}/{} {}/{}. Including: {}", projectCounter, projectSz, artifactCounter, artifactSz, ar );

        if ( ar.isVariableVersion() )
        {
            final ProjectVersionRef specific = discoverer.resolveSpecificVersion( ar, discoveryConfig );
            if ( specific == null )
            {
                logger.error( "No version available for variable reference: {}. Skipping.", ar.asProjectVersionRef() );
                return;
            }

            ar = new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), specific.getVersionSpec(), ar.getType(), ar.getClassifier(), ar.isOptional() );
        }

        logger.debug( "{}/{} {}/{} 1. Resolving referenced artifact: {}", projectCounter, projectSz, artifactCounter, artifactSz, ar );
        final ConcreteResource mainArtifact = addToContent( ar, items, location, excluded, seen );
        if ( mainArtifact == null )
        {
            logger.debug( "Referenced artifact {} was excluded or not resolved. Skip trying pom and type/classifier extras.", ar );
            return;
        }

        // if multi-source GAVs are enabled, use the main location still
        // otherwise, restrict results for this GAV to the place where the main artifact came from.
        final Location artifactLocation = recipe.isMultiSourceGAVs() ? location : mainArtifact.getLocation();

        if ( !"pom".equals( ar.getType() ) )
        {
            final ArtifactRef pomAR = new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), "pom", null, false );

            logger.debug( "{}/{} {}/{} 2. Resolving POM: {}", projectCounter, projectSz, artifactCounter, artifactSz, pomAR );
            addToContent( pomAR, items, artifactLocation, excluded, seen );
        }
        else
        {
            logger.debug( "Referenced artifact: {} WAS a POM. Skipping special POM resolution.", ar );
        }

        final Set<ExtraCT> extras = recipe.getExtras();
        int extCounter = 3;
        if ( extras != null )
        {
            if ( recipe.hasWildcardExtras() )
            {
                // 1. scan for all classifier/type for the GAV
                Map<TypeAndClassifier, ConcreteResource> tcs;
                try
                {
                    tcs = artifacts.listAvailableArtifacts( location, ar.asProjectVersionRef() );
                }
                catch ( final TransferException e )
                {
                    throw new CartoDataException( "Failed to list available type-classifier combinations for: {} from: {}. Reason: {}", e, ar,
                                                  location, e.getMessage() );
                }

                // 2. match up the resulting list against the extras we have

                for ( final Entry<TypeAndClassifier, ConcreteResource> entry : tcs.entrySet() )
                {
                    final TypeAndClassifier tc = entry.getKey();
                    final ConcreteResource res = entry.getValue();

                    if ( isExcluded( res.getLocation() ) )
                    {
                        logger.debug( "EXCLUDED: {}:{} (from: {})", ar, tc, res );
                        continue;
                    }

                    for ( final ExtraCT extra : extras )
                    {
                        if ( extra == null )
                        {
                            continue;
                        }

                        if ( extra.matches( tc ) )
                        {
                            final ArtifactRef extAR = new ArtifactRef( ar, tc, false );
                            logger.debug( "{}/{} {}/{} {}/{}. Attempting to resolve classifier/type artifact from listing: {}", projectCounter,
                                          projectSz, artifactCounter, artifactSz, extCounter, tcs.size(), extAR );

                            // if we're using a listing for wildcards, we've already established that these exist...
                            // so don't waste the time on individual calls!
                            //
                            // addToContent( extAR, items, artifactLocation, excluded, seen );
                            //
                            if ( !seen.contains( extAR ) )
                            {
                                logger.debug( "+ {} (Wildcard addition)(resource: {})", extAR, res );
                                items.put( extAR, res );
                            }
                            else
                            {
                                logger.debug( "- {} (Wildcard; ALREADY SEEN)(resource: {})", extAR, res );
                            }
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
                        new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), extraCT.getType(), extraCT.getClassifier(), false );

                    logger.debug( "{}/{} {}/{} {}/{}. Attempting to resolve specifically listed classifier/type artifact: {}", projectCounter,
                                  projectSz, artifactCounter, artifactSz, extCounter, extras.size(), extAR );
                    addToContent( extAR, items, artifactLocation, excluded, seen );
                    extCounter++;
                }
            }
        }

        final Set<String> metas = recipe.getMetas();
        if ( metas != null && !metas.isEmpty() )
        {
            logger.debug( "Attempting to resolve metadata files for: {}", metas );

            int metaCounter = extCounter;
            final int metaSz = ( items.size() * metas.size() ) + extCounter;
            for ( final Entry<ArtifactRef, ConcreteResource> entry : new HashMap<ArtifactRef, ConcreteResource>( items ).entrySet() )
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

                    logger.debug( "{}/{} {}/{} {}/{}. Attempting to resolve 'meta' artifact: {}", projectCounter, projectSz, artifactCounter,
                                  artifactSz, metaCounter, metaSz, metaAR );
                    addToContent( metaAR, items, artifactLocation, excluded, seen );
                    metaCounter++;
                }
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
                logger.debug( "+ {} (transfer: {})", ar, item );
                items.put( ar, item );
            }
            else
            {
                logger.debug( "- {}", ar );
            }

            return item;
        }
        else
        {
            logger.debug( "- {} (ALREADY SEEN)", ar );
        }

        return null;
    }

    private ConcreteResource resolve( final ArtifactRef ar, final Location location, final Set<Location> excluded, final Set<ArtifactRef> seen )
        throws CartoDataException
    {
        logger.debug( "Attempting to resolve: {} from: {}", ar, location );
        ConcreteResource item;
        try
        {
            item = artifacts.checkExistence( location, ar );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve: {} from: {}. Reason: {}", e, ar, location, e.getMessage() );
        }

        logger.info( "Got: {}", item );

        if ( item == null )
        {
            logger.warn( "NOT FOUND: {}", ar );
        }
        else if ( isExcluded( item.getLocation() ) )
        {
            logger.debug( "EXCLUDED: {} (Location was: {})", ar, item.getLocation() );
            return null;
        }

        return item;
    }

    @Override
    public String toString()
    {
        return super.toString() + "(" + name + ")";
    }

    public String getName()
    {
        return name;
    }

    private boolean isExcluded( final Location location )
    {
        return excluded != null && excluded.contains( location );
    }

    public void setLatch( final CountDownLatch latch )
    {
        this.latch = latch;
    }

    public ProjectVersionRef getRef()
    {
        return ref;
    }

    public Map<ArtifactRef, ConcreteResource> getItems()
    {
        return items;
    }

    public Map<ArtifactRef, CartoDataException> getErrors()
    {
        return errors;
    }
}
