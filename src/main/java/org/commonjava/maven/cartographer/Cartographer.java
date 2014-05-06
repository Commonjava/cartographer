/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;

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

    protected Cartographer()
    {
    }

    public Cartographer( final CalculationOps calculator, final GraphOps grapher, final GraphRenderingOps renderer,
                         final MetadataOps metadata, final ResolveOps resolver,
                         final RelationshipGraphFactory graphFactory )
    {
        this.calculator = calculator;
        this.grapher = grapher;
        this.renderer = renderer;
        this.metadata = metadata;
        this.resolver = resolver;
        this.graphFactory = graphFactory;
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
