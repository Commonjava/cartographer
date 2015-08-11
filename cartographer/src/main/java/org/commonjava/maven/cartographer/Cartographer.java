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
package org.commonjava.maven.cartographer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.*;
import org.commonjava.maven.galley.maven.GalleyMaven;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class Cartographer
{

    @Inject
    protected CalculationOps calculator;

    @Inject
    protected GraphOps grapher;

    @Inject
    protected GraphRenderingOps renderer;

    @Inject
    protected MetadataOps metadata;

    @Inject
    protected ResolveOps resolver;

    @Inject
    protected RelationshipGraphFactory graphFactory;

    private GalleyMaven galleyMaven;

    @Inject
    protected ObjectMapper objectMapper;

    public Cartographer( final GalleyMaven galleyMaven, final CalculationOps calculator, final GraphOps grapher,
                         final GraphRenderingOps renderer,
                         final MetadataOps metadata, final ResolveOps resolver,
                         final RelationshipGraphFactory graphFactory, final ObjectMapper objectMapper )
    {
        this.galleyMaven = galleyMaven;
        this.calculator = calculator;
        this.grapher = grapher;
        this.renderer = renderer;
        this.metadata = metadata;
        this.resolver = resolver;
        this.graphFactory = graphFactory;
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public GalleyMaven getGalley()
    {
        return galleyMaven;
    }

    public CalculationOps getCalculator()
    {
        return calculator;
    }

    public GraphOps getGrapher()
    {
        return grapher;
    }

    public GraphRenderingOps getRenderer()
    {
        return renderer;
    }

    public MetadataOps getMetadata()
    {
        return metadata;
    }

    public ResolveOps getResolver()
    {
        return resolver;
    }

    public RelationshipGraphFactory getGraphFactory()
    {
        return graphFactory;
    }

    public void close()
        throws CartoDataException
    {
        try
        {
            graphFactory.close();
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to close graph factory. Reason: {}", e, e.getMessage() );
        }
    }

}
