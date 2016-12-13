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

import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.request.SingleGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.GraphController;
import org.commonjava.cartographer.result.GraphExport;
import org.commonjava.cartographer.result.MappedProjectsResult;
import org.commonjava.cartographer.result.ProjectErrors;
import org.commonjava.cartographer.result.ProjectListResult;
import org.commonjava.cartographer.result.ProjectPathsResult;
import org.commonjava.cartographer.graph.traverse.model.BuildOrder;
import org.commonjava.propulsor.deploy.resteasy.RestResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.*;

@Path( "/api/depgraph/graph" )
@Consumes( { application_json } )
@Produces( { application_json } )
@ApplicationScoped
public class GraphResource
        implements RestResources
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphController controller;

    @Path( "/paths" )
    @POST
    public ProjectPathsResult getPaths( final PathsRequest recipe )
    {
        try
        {
            return controller.getPaths( recipe );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/errors" )
    @POST
    public ProjectErrors errors( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.errors( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }

    @Path( "/reindex" )
    @POST
    public ProjectListResult reindex( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.reindex( recipe );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/incomplete" )
    @POST
    public ProjectListResult incomplete( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.incomplete( recipe );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/variable" )
    @POST
    public ProjectListResult variable( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.variable( recipe );
        }
        catch ( final CartoRESTException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/ancestry" )
    @POST
    public MappedProjectsResult ancestryOf( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.ancestryOf( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }

    @Path( "/build-order" )
    @POST
    public BuildOrder buildOrder( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.buildOrder( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }

    @Path( "/export" )
    @POST
    public GraphExport graph( final SingleGraphRequest recipe )
    {
        try
        {
            return controller.projectGraph( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }

}
