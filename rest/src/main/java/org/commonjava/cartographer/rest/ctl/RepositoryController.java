/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.rest.ctl;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.ResolveOps;
import org.commonjava.cartographer.rest.dto.DownlogRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.util.RecipeHelper;
import org.commonjava.cartographer.rest.dto.ArtifactRepoContent;
import org.commonjava.cartographer.rest.dto.DownlogResult;
import org.commonjava.cartographer.rest.dto.ProjectRepoContent;
import org.commonjava.cartographer.rest.dto.RepoContentResult;
import org.commonjava.cartographer.rest.dto.UrlMapProject;
import org.commonjava.cartographer.rest.dto.UrlMapResult;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.maven.util.ArtifactPathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferBatch;
import org.commonjava.propulsor.deploy.undertow.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

@ApplicationScoped
public class RepositoryController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolveOps ops;

    @Inject
    private TransferManager transferManager;

    @Inject
    private RecipeHelper configHelper;

    @Inject
    private TypeMapper typeMapper;

    public UrlMapResult getUrlMap( final InputStream configStream, final String baseUri )
            throws CartoRESTException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( configStream, RepositoryContentRequest.class );
        return getUrlMap( dto, baseUri );
    }

    public UrlMapResult getUrlMap( final String json, final String baseUri )
                    throws CartoRESTException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( json, RepositoryContentRequest.class );
        return getUrlMap( dto, baseUri );
    }

    public UrlMapResult getUrlMap( final RepositoryContentRequest recipe, final String baseUri )
                    throws CartoRESTException
    {
        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( recipe );
        return new UrlMapResult( createUrlMapProjectMappings( contents, recipe, baseUri ) );
    }

    public DownlogResult getDownloadLog( final InputStream configStream, final String baseUri )
                    throws CartoRESTException
    {
        final DownlogRequest dto = configHelper.readDownlogDTO( configStream );
        return getDownloadLog( dto, baseUri );
    }

    public DownlogResult getDownloadLog( final String json, final String baseUri )
                    throws CartoRESTException
    {
        final DownlogRequest dto = configHelper.readDownlogDTO( json );
        return getDownloadLog( dto, baseUri );
    }

    public DownlogResult getDownloadLog( final DownlogRequest recipe, final String baseUri )
                    throws CartoRESTException
    {
        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( recipe );
        return new DownlogResult( recipe.getLinePrefix(), formatDownlogLines( contents, recipe, baseUri ) );
    }

    public void getZipRepository( final InputStream configStream, final OutputStream zipStream )
                    throws CartoRESTException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( configStream, RepositoryContentRequest.class );
        getZipRepository( dto, zipStream );
    }

    public void getZipRepository( final String json, final OutputStream zipStream )
                    throws CartoRESTException
    {
        final RepositoryContentRequest dto = configHelper.readRecipe( json, RepositoryContentRequest.class );
        getZipRepository( dto, zipStream );
    }

    public void getZipRepository( final RepositoryContentRequest dto, final OutputStream zipStream )
                    throws CartoRESTException
    {
        ZipOutputStream stream = null;
        try
        {
            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto );

            final Set<ConcreteResource> entries = new HashSet<>();
            final Set<String> seenPaths = new HashSet<>();

            logger.info( "Iterating contents with {} GAVs.", contents.size() );
            for ( final Map<ArtifactRef, ConcreteResource> artifactResources : contents.values() )
            {
                for ( final Entry<ArtifactRef, ConcreteResource> entry : artifactResources.entrySet() )
                {
                    final ArtifactRef ref = entry.getKey();
                    final ConcreteResource resource = entry.getValue();

                    //                        logger.info( "Checking {} ({}) for inclusion...", ref, resource );

                    final String path = resource.getPath();
                    if ( seenPaths.contains( path ) )
                    {
                        logger.warn( "Conflicting path: {}. Skipping {}.", path, ref );
                        continue;
                    }

                    seenPaths.add( path );

                    //                        logger.info( "Adding to batch: {} via resource: {}", ref, resource );
                    entries.add( resource );
                }
            }

            logger.info( "Starting batch retrieval of {} artifacts.", entries.size() );
            TransferBatch batch = new TransferBatch( entries );
            batch = transferManager.batchRetrieve( batch, new EventMetadata() );

            logger.info( "Retrieved {} artifacts. Creating zip.", batch.getTransfers().size() );

            // FIXME: Stream to a temp file, then pass that to the Response.ok() handler...
            stream = new ZipOutputStream( zipStream );

            final List<Transfer> items = new ArrayList<>( batch.getTransfers().values() );
            Collections.sort( items, (f, s)-> f.getPath().compareTo( s.getPath() ) );

            for ( final Transfer item : items )
            {
                //                    logger.info( "Adding: {}", item );
                if ( item != null )
                {
                    final String path = item.getPath();
                    final ZipEntry ze = new ZipEntry( path );
                    stream.putNextEntry( ze );

                    InputStream itemStream = null;
                    try
                    {
                        itemStream = item.openInputStream();
                        copy( itemStream, stream );
                    }
                    finally
                    {
                        closeQuietly( itemStream );
                    }
                }
            }
        }
        catch ( final IOException | TransferException e )
        {
            throw new CartoRESTException( "Failed to generate runtime repository. Reason: {}", e, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }
    }

    public RepoContentResult getRepoContent( RepositoryContentRequest request, String baseUri )
            throws CartoRESTException
    {
        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( request );
        RepoContentResult result = new RepoContentResult();
        for ( ProjectVersionRef key: contents.keySet() )
        {
            ProjectRepoContent projectContent = new ProjectRepoContent();
            result.addProject( key, projectContent );

            Map<ArtifactRef, ConcreteResource> artifactMap = contents.get( key );
            for ( ArtifactRef artifact: artifactMap.keySet() )
            {
                ConcreteResource item = artifactMap.get( artifact );
                Location location = item.getLocation();

                String baseUrl = formatRepositoryUrl( location, baseUri );
                result.addRepoUrl( location.getName(), baseUrl );
                try
                {
                    String path = ArtifactPathUtils.formatArtifactPath( artifact, typeMapper );
                    projectContent.addArtifact( new ArtifactRepoContent( artifact, location.getName(), path ) );
                }
                catch ( TransferException e )
                {
                    logger.error( "Failed to format artifact path: %s. Reason: %s", e, artifact, e.getMessage() );
                }
            }
        }

        return result;
    }

    private Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveContents(
                    final RepositoryContentRequest recipe )
                    throws CartoRESTException
    {
        configHelper.setRecipeDefaults( recipe );

        Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents;
        try
        {
            contents = ops.resolveRepositoryContents( recipe );
        }
        catch ( final CartoDataException e )
        {
            logger.error( String.format( "Failed to graph repository contents for: %s. Reason: %s", recipe,
                                         e.getMessage() ), e );
            throw new CartoRESTException( "Failed to graph repository contents for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s",
                                          e, recipe, e.getMessage() );
        }

        return contents;
    }

    private Map<ProjectVersionRef,UrlMapProject> createUrlMapProjectMappings(
                    Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents,
                    RepositoryContentRequest recipe, String baseUri )
    {
        Map<ProjectVersionRef, UrlMapProject> map = new HashMap<>();
        for ( final ProjectVersionRef gav : contents.keySet() )
        {
            final Map<ArtifactRef, ConcreteResource> items = contents.get( gav );

            final Set<String> files = new HashSet<>();
            Location location = null;

            for ( final ConcreteResource item : items.values() )
            {
                final Location loc = item.getLocation();

                // FIXME: we're squashing some potential variation in the locations here!
                // if we're not looking for local urls, allow any cache-only location to be overridden...
                if ( location == null )
                {
                    location = loc;
                }

                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.debug( "Adding {} (keyLocation: {})", item, location );
                files.add( new File( item.getPath() ).getName() );
            }

            final Set<String> sortedFiles = new TreeSet<>( files );

            final String url = formatRepositoryUrl( location, baseUri );

            map.put( gav, new UrlMapProject( url, sortedFiles ) );
        }

        return map;
    }

    private String formatRepositoryUrl( final Location location, final String baseUri )
    {
        return location.getUri();
    }

    private Set<String> formatDownlogLines( Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents,
                                            DownlogRequest recipe, String baseUri )
                    throws CartoRESTException
    {
        Set<String> locations = new TreeSet<>();
        for ( final ProjectVersionRef ref : contents.keySet() )
        {
            final Map<ArtifactRef, ConcreteResource> items = contents.get( ref );
            for ( final ConcreteResource item : items.values() )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.debug( "Adding: '{}'", item );
                locations.add( formatDownlogEntry( item, recipe, baseUri ) );
            }
        }

        return locations;
    }

    private String formatDownlogEntry( ConcreteResource item, DownlogRequest recipe, String baseUri )
                    throws CartoRESTException
    {
        String path;
        if ( recipe.isPathOnly() )
        {
            path = item.getPath();
        }
        else
        {
            try
            {
                path = buildUrl( item.getLocation().getUri(), item.getPath() );
            }
            catch ( MalformedURLException e )
            {
                throw new CartoRESTException( "Failed to generate remote URL for: %s in location: %s. Reason: %s",
                                                  e, item.getPath(), item.getLocationUri(), e.getMessage() );
            }
        }

        return path;
    }

}
