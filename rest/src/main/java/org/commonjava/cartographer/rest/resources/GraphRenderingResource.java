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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.cartographer.request.MultiRenderRequest;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.RenderingController;
import org.commonjava.propulsor.deploy.resteasy.RestResources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.File;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.*;

@Api( value = "Graph Rendering Resource", description = "Graph Rendering Resource." )
@Path( "/api/depgraph/render" )
@ApplicationScoped
public class GraphRenderingResource
        implements RestResources
{

    private static final String TYPE_GRAPHVIZ = "text/x-graphviz";

    @Inject
    private RenderingController controller;

    @ApiOperation( "pom." )
    @ApiResponses( { @ApiResponse( code = 200, response = String.class, message = "pom" ) } )
    @Path( "/pom" )
    @POST
//    @Produces( application_xml )
    public String pom( PomRequest recipe )
    {
        try
        {
            return controller.pomFor( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }

    @ApiOperation( "dotfile." )
    @ApiResponses( { @ApiResponse( code = 200, response = String.class, message = "dotfile" ) } )
    @Path( "/dotfile" )
    @POST
    @Produces( TYPE_GRAPHVIZ )
    public String dotfile( final MultiRenderRequest recipe )
    {
        try
        {
            return controller.dotfile( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }

    @ApiOperation( "tree." )
    @ApiResponses( { @ApiResponse( code = 200, response = File.class, message = "tree" ) } )
    @Path( "/depTree" )
    @POST
    @Produces( text_plain )
    public File tree( final RepositoryContentRequest recipe )
    {
        try
        {
            return controller.tree( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }

    @ApiOperation( "list." )
    @ApiResponses( { @ApiResponse( code = 200, response = File.class, message = "file" ) } )
    @Path( "/depList" )
    @POST
    @Produces( text_plain )
    public File list( final RepositoryContentRequest recipe )
    {
        try
        {
            return controller.list( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }
}
