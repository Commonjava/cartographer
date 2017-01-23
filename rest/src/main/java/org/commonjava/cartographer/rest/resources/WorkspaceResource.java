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
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.WorkspaceController;
import org.commonjava.cartographer.rest.dto.WorkspaceList;
import org.commonjava.propulsor.deploy.resteasy.RestResources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static org.commonjava.cartographer.rest.util.ResponseUtils.formatResponse;
import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.*;

@Api( value = "Workspace Resource", description = "Workspace Resource." )
@Path( "/api/depgraph/ws" )
@Produces( { application_json } )
@ApplicationScoped
public class WorkspaceResource
        implements RestResources
{

    @Inject
    private WorkspaceController controller;

    @ApiOperation( "Delete workspace." )
    @ApiResponses( { @ApiResponse( code = 204, message = "Workspace deleted" ) } )
    @Path( "/{wsid}" )
    @DELETE
    public Response delete( final @PathParam( "wsid" ) String id )
    {
        Response response;
        try
        {
            controller.delete( id );
            response = Response.noContent()
                               .build();
        }
        catch ( final CartoRESTException e )
        {
            response = formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "List workspaces." )
    @ApiResponses( { @ApiResponse( code = 200, response = WorkspaceList.class, message = "Workspaces" ) } )
    @GET
    public WorkspaceList list()
    {
        try
        {
            return controller.list();
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }
}
