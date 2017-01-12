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
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.GraphCalculation;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.application_json;

@Api( value = "Calculator Resource", description = "Calculator Resource." )
@Path( "/api/depgraph/calc" )
@Consumes( { "application/json", "application/indy*+json" } )
@Produces( { "application/json", "application/indy*+json" } )
@ApplicationScoped
public class CalculatorResource
        implements RestResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CalculatorController controller;

    @ApiOperation( "Get Graph Difference." )
    @ApiResponses( { @ApiResponse( code = 200, response = GraphDifference.class, message = "Graph Difference" ),
            @ApiResponse( code = 404, message = "Not found" ) } )
    @Path( "/diff" )
    @POST
    @Produces( application_json )
    public GraphDifference<ProjectRelationship<?, ?>> difference( final GraphAnalysisRequest request )
    {
        Response response = null;
        try
        {
            return controller.difference( request );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @ApiOperation( "Drift Graph Difference." )
    @ApiResponses( { @ApiResponse( code = 200, response = GraphDifference.class, message = "Graph Difference" ),
            @ApiResponse( code = 404, message = "Not found" ) } )
    @Path( "/drift" )
    @POST
    @Produces( application_json )
    public GraphDifference<ProjectVersionRef> drift( final GraphAnalysisRequest request )
    {
        Response response = null;
        try
        {
            return controller.drift( request );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @ApiOperation( "Graph Calculation." )
    @ApiResponses( { @ApiResponse( code = 200, response = GraphCalculation.class, message = "Graph Calculation" ),
            @ApiResponse( code = 404, message = "Not found" ) } )
    @Path( "/calculate" )
    @POST
    @Produces( application_json )
    public GraphCalculation calculate( MultiGraphRequest request )
    {
        try
        {
            return controller.calculate( request );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }
}
