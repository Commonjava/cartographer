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
import org.commonjava.cartographer.CartoException;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.GraphCalculation;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.request.SourceAliasRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.AdminController;
import org.commonjava.cartographer.rest.ctl.CalculatorController;
import org.commonjava.cartographer.result.GraphDifference;
import org.commonjava.cartographer.result.SourceAliasMapResult;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.propulsor.deploy.resteasy.RestResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.Map;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.application_json;

@Api( value = "Admin Resource", description = "Admin Resource." )
@Path( "/api/admin" )
@Consumes( { "application/json" } )
@Produces( { "application/json" } )
@ApplicationScoped
public class AdminResource
        implements RestResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AdminController controller;

    @ApiOperation( "Retrieve the source aliases." )
    @ApiResponses( { @ApiResponse( code = 200, response = SourceAliasMapResult.class, message = "Source aliases" ),
            @ApiResponse( code = 404, message = "Not found" ) } )
    @Path( "/sources/aliases" )
    @GET
    @Produces( application_json )
    public SourceAliasMapResult getSourceAliasMap()
    {
        try
        {
            return new SourceAliasMapResult( controller.getSourceAliasMap() );
        }
        catch ( CartoException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @ApiOperation( "Add source alias." )
    @ApiResponses( { @ApiResponse( code = 200, message = "Source alias added" ),
            @ApiResponse( code = 409, message = "Conflict" ) } )
    @Path( "/sources/aliases" )
    @POST
    @Produces( application_json )
    public Response aliasSource( SourceAliasRequest request )
    {
        try
        {
            if ( controller.addSourceAlias( request.getAlias(), request.getUrl() ) )
            {
                return Response.ok().build();
            }
            else
            {
                return Response.status( Response.Status.CONFLICT ).build();
            }
        }
        catch ( CartoException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

}
