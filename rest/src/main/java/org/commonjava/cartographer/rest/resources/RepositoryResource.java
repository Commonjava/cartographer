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
package org.commonjava.cartographer.rest.resources;

import org.commonjava.cartographer.rest.dto.DownlogRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.RepositoryController;
import org.commonjava.cartographer.rest.dto.DownlogResult;
import org.commonjava.cartographer.rest.dto.RepoContentResult;
import org.commonjava.cartographer.rest.dto.UrlMapResult;
import org.commonjava.propulsor.deploy.resteasy.RestResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import static org.commonjava.cartographer.rest.util.ResponseUtils.*;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.*;

@Path( "/api/depgraph/repo" )
@Consumes( { "application/json", "application/indy*+json" } )
@ApplicationScoped
public class RepositoryResource
        implements RestResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RepositoryController controller;

    @Path( "/content" )
    @Produces( { "application/json", "application/indy*+json" } )
    @POST
    public RepoContentResult getRepoContent( final RepositoryContentRequest request, final @Context UriInfo uriInfo )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder().path( "api" ).build().toString();

            return controller.getRepoContent( request, baseUri );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return null;
    }

    @Path( "/urlmap" )
    @Produces( { "application/json", "application/indy*+json" } )
    @POST
    public UrlMapResult getUrlMap( final RepositoryContentRequest request, final @Context UriInfo uriInfo )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder().path( "api" ).build().toString();

            return controller.getUrlMap( request, baseUri );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return null;
    }

    @Path( "/downlog" )
    @POST
    @Produces( text_plain )
    public DownlogResult getDownloadLog( final DownlogRequest request, final @Context UriInfo uriInfo )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder().path( "api" ).build().toString();

            return controller.getDownloadLog( request, baseUri );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }

        return null;
    }

    @Path( "/zip" )
    @POST
    @Produces( application_zip )
    public StreamingOutput getZipRepository( RepositoryContentRequest request )
    {
        return ( output ) -> {
            try
            {
                controller.getZipRepository( request, output );
            }
            catch ( final CartoRESTException e )
            {
                throwError( e );
            }
        };
    }
}
