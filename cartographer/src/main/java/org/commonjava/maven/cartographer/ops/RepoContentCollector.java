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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.request.ExtraCT;
import org.commonjava.maven.cartographer.request.RepositoryContentRequest;
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

    private final RepositoryContentRequest recipe;

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

    public RepoContentCollector( final ProjectVersionRef ref, final ProjectRefCollection refs,
                                 final RepositoryContentRequest recipe, final Location location,
                                 final DiscoveryConfig discoveryConfig, final ArtifactManager artifacts,
                                 final ProjectRelationshipDiscoverer discoverer, final Set<Location> excluded,
                                 final int projectCounter, final int projectSz )
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
                    logger.error( String.format( "ERROR for %s: %s", ar, e.getMessage() ), e );
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
            // FIXME: If the version is resolved using metadata from an excluded location, we've got a problem!
            final ProjectVersionRef specific = discoverer.resolveSpecificVersion( ar, discoveryConfig );
            if ( specific == null )
            {
                throw new CartoDataException( "No version available for variable reference: {}. Skipping all but POM.",
                        ar.asProjectVersionRef() );
            }

            ar =
                new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), specific.getVersionSpec(), ar.getType(),
                                 ar.getClassifier(), ar.isOptional() );
        }

        ArtifactRef pomAR;
        if ( !"pom".equals( ar.getType() ) )
        {
            pomAR = ar.asPomArtifact();

            logger.debug( "{}/{} {}/{} 1. Resolving POM: {}", projectCounter, projectSz, artifactCounter, artifactSz,
                          pomAR );
        }
        else
        {
            logger.debug( "{}/{} {}/{} 1. Referenced artifact: {} WAS a POM. Skipping special POM resolution.",
                          projectCounter, projectSz, artifactCounter, artifactSz, ar );
            pomAR = ar;
        }

        ConcreteResource pomArtifact = addToContent( pomAR, items, location, excluded, seen );
        if ( pomArtifact == null )
        {
            // if the POM was resolved because of a reference to another jar, war, etc. then it'll be in the items map...look here before giving up.
            pomArtifact = items.get( pomAR );
            if ( pomArtifact == null )
            {
                throw new CartoDataException( "Failed to resolve POM content: {}. Skipping associated artifacts.",
                                              pomAR );
            }
        }

        // *********************************
        // From here on, we use the resolved location of the POM, unless we're using multi-sourced GAV resolution.
        // *********************************

        // if multi-source GAVs are enabled, use the main location still
        // otherwise, restrict results for this GAV to the place where the main artifact came from.
        final Location resolvedLocation = recipe.isMultiSourceGAVs() ? location : pomArtifact.getLocation();

        if ( ar.equals( pomAR ) )
        {
            logger.debug( "{}/{} {}/{} 2. Given artifact is a POM, which was already resolved. Skipping this second reference." );
        }
        else
        {
            logger.debug( "{}/{} {}/{} 2. Resolving referenced artifact: {}", projectCounter, projectSz,
                          artifactCounter, artifactSz, ar );
            final ConcreteResource mainArtifact = addToContent( ar, items, resolvedLocation, excluded, seen );
            if ( mainArtifact == null )
            {
                throw new CartoDataException(
                                              "Referenced artifact {} was excluded or not resolved. Skip trying pom and type/classifier extras.",
                                              ar );
            }
        }

        final Set<ExtraCT> extras = recipe.getExtras();
        final int extOffset = 3;
        int extCounter = extOffset;
        if ( extras != null )
        {
            if ( recipe.hasWildcardExtras() )
            {
                // 1. scan for all classifier/type for the GAV
                Map<TypeAndClassifier, ConcreteResource> tcs;
                try
                {
                    tcs = artifacts.listAvailableArtifacts( resolvedLocation, ar.asProjectVersionRef() );
                }
                catch ( final TransferException e )
                {
                    throw new CartoDataException(
                                                  "Failed to list available type-classifier combinations for: {} from: {}. Reason: {}",
                                                  e, ar, resolvedLocation, e.getMessage() );
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
                            logger.debug( "{}/{} {}/{} {}/{}. Attempting to resolve classifier/type artifact from listing: {}",
                                          projectCounter, projectSz, artifactCounter, artifactSz, extCounter,
                                          tcs.size() + extOffset, extAR );

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
                        new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), extraCT.getType(),
                                         extraCT.getClassifier(), false );

                    logger.debug( "{}/{} {}/{} {}/{}. Attempting to resolve specifically listed classifier/type artifact: {}",
                                  projectCounter, projectSz, artifactCounter, artifactSz, extCounter, extras.size()
                                      + extOffset, extAR );
                    addToContent( extAR, items, resolvedLocation, excluded, seen );
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

                    logger.debug( "{}/{} {}/{} {}/{}. Attempting to resolve 'meta' artifact: {}", projectCounter,
                                  projectSz, artifactCounter, artifactSz, metaCounter, metaSz, metaAR );
                    addToContent( metaAR, items, resolvedLocation, excluded, seen );
                    metaCounter++;
                }
            }
        }
    }

    private ConcreteResource addToContent( final ArtifactRef ar, final Map<ArtifactRef, ConcreteResource> items,
                                           final Location location, final Set<Location> excluded,
                                           final Set<ArtifactRef> seen )
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

    private ConcreteResource resolve( final ArtifactRef ar, final Location location, final Set<Location> excluded,
                                      final Set<ArtifactRef> seen )
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
            return null;
            //            throw new CartoDataException( "Cannot find: {} in: {}", ar, location );
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
