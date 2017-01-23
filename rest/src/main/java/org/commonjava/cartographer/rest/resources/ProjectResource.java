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
import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.ProjectController;
import org.commonjava.cartographer.result.MappedProjectRelationshipsResult;
import org.commonjava.cartographer.result.MappedProjectResult;
import org.commonjava.cartographer.result.ProjectListResult;
import org.commonjava.propulsor.deploy.resteasy.RestResources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.*;

@Api( value = "Project Resource", description = "Project Resource." )
@Path( "/api/depgraph/project" )
@Consumes( { application_json } )
@Produces( { application_json } )
@ApplicationScoped
public class ProjectResource
        implements RestResources
{
    @Inject
    private ProjectController controller;

    @ApiOperation( "List projects." )
    @ApiResponses( { @ApiResponse( code = 200, response = ProjectListResult.class, message = "Project List Result" ) } )
    @Path( "/list" )
    @POST
    public ProjectListResult list( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.list( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        // not used; throwError will supercede with WebApplicationException
        return null;
    }

    @ApiOperation( "Map Project." )
    @ApiResponses( { @ApiResponse( code = 200, response = MappedProjectResult.class, message = "Mapped Project" ) } )
    @Path( "/parents" )
    @POST
    public MappedProjectResult parentOf( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.parentOf( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }

    @ApiOperation( "Dependencies Of." )
    @ApiResponses( { @ApiResponse( code = 200, response = MappedProjectRelationshipsResult.class, message = "Mapped Project Relationships" ) } )
    @Path( "/relationships" )
    @POST
    public MappedProjectRelationshipsResult dependenciesOf( final ProjectGraphRelationshipsRequest recipe )
    {
        try
        {
            return controller.relationshipsDeclaredBy( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }

    @ApiOperation( "Relationships Targeting." )
    @ApiResponses( { @ApiResponse( code = 200, response = MappedProjectRelationshipsResult.class, message = "Mapped Project Relationships" ) } )
    @Path( "/targeting" )
    @POST
    public MappedProjectRelationshipsResult relationshipsTargeting( final ProjectGraphRelationshipsRequest recipe )
    {
        try
        {
            return controller.relationshipsTargeting( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }

}
