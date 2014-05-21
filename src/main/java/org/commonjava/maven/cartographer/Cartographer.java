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

import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.ops.WorkspaceOps;

@ApplicationScoped
public class Cartographer
{

    @Inject
    private CartoDataManager database;

    @Inject
    CalculationOps calculator;

    @Inject
    private GraphOps grapher;

    @Inject
    private GraphRenderingOps renderer;

    @Inject
    private MetadataOps metadata;

    @Inject
    ResolveOps resolver;

    @Inject
    private WorkspaceOps workspaces;

    protected Cartographer()
    {
    }

    public Cartographer( final CartoDataManager database, final CalculationOps calculator, final GraphOps grapher,
                         final GraphRenderingOps renderer, final MetadataOps metadata, final ResolveOps resolver,
                         final WorkspaceOps workspace )
    {
        this.database = database;
        this.calculator = calculator;
        this.grapher = grapher;
        this.renderer = renderer;
        this.metadata = metadata;
        this.resolver = resolver;
        this.workspaces = workspace;
    }

    public CartoDataManager getDatabase()
    {
        return database;
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

    public WorkspaceOps getWorkspaces()
    {
        return workspaces;
    }

}
