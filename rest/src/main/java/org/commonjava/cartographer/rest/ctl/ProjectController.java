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
import org.commonjava.cartographer.ops.GraphOps;
import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.util.RecipeHelper;
import org.commonjava.cartographer.result.MappedProjectRelationshipsResult;
import org.commonjava.cartographer.result.MappedProjectResult;
import org.commonjava.cartographer.result.ProjectListResult;
import org.commonjava.propulsor.deploy.undertow.util.ApplicationStatus;

import javax.inject.Inject;
import java.io.InputStream;

public class ProjectController
{
    @Inject
    private GraphOps ops;

    @Inject
    private RecipeHelper configHelper;

    public ProjectListResult list( final InputStream stream )
            throws CartoRESTException
    {
        final ProjectGraphRequest recipe = configHelper.readRecipe( stream, ProjectGraphRequest.class );
        return list( recipe );
    }

    public ProjectListResult list( final ProjectGraphRequest recipe )
        throws CartoRESTException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.listProjects( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException( "Failed to lookup project listing matching request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                          recipe, e.getMessage() );
        }
    }

    public MappedProjectResult parentOf( final InputStream stream )
        throws CartoRESTException
    {
        final ProjectGraphRequest recipe = configHelper.readRecipe( stream, ProjectGraphRequest.class );
        return parentOf( recipe );
    }

    public MappedProjectResult parentOf( final ProjectGraphRequest recipe )
        throws CartoRESTException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.getProjectParent( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException( "Failed to lookup parent(s) for request: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public MappedProjectRelationshipsResult relationshipsDeclaredBy( final InputStream stream )
        throws CartoRESTException
    {
        final ProjectGraphRelationshipsRequest recipe =
            configHelper.readRecipe( stream, ProjectGraphRelationshipsRequest.class );

        return relationshipsDeclaredBy( recipe );
    }

    public MappedProjectRelationshipsResult relationshipsDeclaredBy( final ProjectGraphRelationshipsRequest recipe )
        throws CartoRESTException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.getDirectRelationshipsFrom( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException(
                                              "Failed to lookup relationships declared by GAVs given in request: {}. Reason: {}",
                                              e, recipe, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public MappedProjectRelationshipsResult relationshipsTargeting( final InputStream stream )
        throws CartoRESTException
    {
        final ProjectGraphRelationshipsRequest recipe =
            configHelper.readRecipe( stream, ProjectGraphRelationshipsRequest.class );

        return relationshipsDeclaredBy( recipe );
    }

    public MappedProjectRelationshipsResult relationshipsTargeting( final ProjectGraphRelationshipsRequest recipe )
        throws CartoRESTException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.getDirectRelationshipsTo( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException(
                                              "Failed to lookup relationships targeting GAVs given in request: {}. Reason: {}",
                                              e, recipe, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

}
