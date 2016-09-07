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
package org.commonjava.cartographer.client;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.GraphOps;
import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.request.SingleGraphRequest;
import org.commonjava.cartographer.result.GraphExport;
import org.commonjava.cartographer.result.MappedProjectRelationshipsResult;
import org.commonjava.cartographer.result.MappedProjectResult;
import org.commonjava.cartographer.result.MappedProjectsResult;
import org.commonjava.cartographer.result.ProjectErrors;
import org.commonjava.cartographer.result.ProjectListResult;
import org.commonjava.cartographer.result.ProjectPathsResult;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.propulsor.client.http.ClientHttpException;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientGraphOps
        implements GraphOps
{
    private ClientCartographer carto;

    private CartographerRESTClient module;

    public ClientGraphOps( ClientCartographer carto, CartographerRESTClient module )
    {
        this.carto = carto;
        this.module = module;
    }

    @Override
    public ProjectListResult listProjects( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.list( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectPathsResult getPaths( PathsRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.paths( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectErrors getProjectErrors( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.errors( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectResult getProjectParent( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.parents( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectRelationshipsResult getDirectRelationshipsFrom( ProjectGraphRelationshipsRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.relationshipsDeclaredBy( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectRelationshipsResult getDirectRelationshipsTo( ProjectGraphRelationshipsRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.relationshipsTargeting( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult reindex( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.reindex( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult getIncomplete( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.incomplete( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult getVariable( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.variable( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectsResult getAncestry( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.ancestors( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public BuildOrder getBuildOrder( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.buildOrder( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public GraphExport exportGraph( SingleGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.export( carto.normalizeRequest( request ) );
        }
        catch ( CartoClientException | ClientHttpException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
