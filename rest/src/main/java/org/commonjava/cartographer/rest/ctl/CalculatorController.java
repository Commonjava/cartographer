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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.CalculationOps;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.GraphCalculation;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.util.RecipeHelper;
import org.commonjava.cartographer.result.GraphDifference;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.propulsor.deploy.undertow.util.ApplicationStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.InputStream;

@ApplicationScoped
public class CalculatorController
{

    @Inject
    private CalculationOps ops;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private RecipeHelper configHelper;

    public GraphDifference<ProjectRelationship<?, ?>> difference( final InputStream configStream )
                    throws CartoRESTException
    {
        final GraphAnalysisRequest request = configHelper.readRecipe( configStream, GraphAnalysisRequest.class );
        return difference( request );
    }

    public GraphDifference<ProjectRelationship<?, ?>> difference( final String json )
            throws CartoRESTException
    {
        final GraphAnalysisRequest request = configHelper.readRecipe( json, GraphAnalysisRequest.class );
        return difference( request );
    }

    public GraphDifference<ProjectRelationship<?, ?>> difference( final GraphAnalysisRequest request )
                    throws CartoRESTException
    {
        try
        {
            return ops.difference( request );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException( "Failed to retrieve graph(s): {}", e, e.getMessage() );
        }
        catch ( final CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Failed to retrieve graph(s): {}",
                                              e, e.getMessage() );
        }
    }

    public GraphDifference<ProjectVersionRef> drift( final InputStream configStream )
                    throws CartoRESTException
    {
        final GraphAnalysisRequest request = configHelper.readRecipe( configStream, GraphAnalysisRequest.class );
        return drift( request );
    }

    public GraphDifference<ProjectVersionRef> drift( final String json )
                    throws CartoRESTException
    {
        final GraphAnalysisRequest request = configHelper.readRecipe( json, GraphAnalysisRequest.class );
        return drift( request );
    }

    public GraphDifference<ProjectVersionRef> drift( final GraphAnalysisRequest request )
                    throws CartoRESTException
    {
        try
        {
            return ops.intersectingTargetDrift( request );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException( "Failed to retrieve graph(s): {}", e, e.getMessage() );
        }
        catch ( final CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Failed to retrieve graph(s): {}",
                                          e, e.getMessage() );
        }
    }

    public GraphCalculation calculate( final InputStream configStream, final String encoding, final String workspaceId )
                    throws CartoRESTException
    {
        final MultiGraphRequest request = configHelper.readRecipe( configStream, MultiGraphRequest.class );
        return calculate( request );
    }

    public GraphCalculation calculate( final String json )
                    throws CartoRESTException
    {
        final MultiGraphRequest request = configHelper.readRecipe( json, MultiGraphRequest.class );
        return calculate( request );
    }

    public GraphCalculation calculate( final MultiGraphRequest request )
                    throws CartoRESTException
    {
        try
        {
            return ops.calculate( request );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException( "Failed to retrieve graph(s): {}", e, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Failed to retrieve graph(s): {}",
                                              e, e.getMessage() );
        }
    }

}
