/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

    @Inject CalculationOps calculator;

    @Inject
    private GraphOps grapher;

    @Inject
    private GraphRenderingOps renderer;

    @Inject
    private MetadataOps metadata;

    @Inject ResolveOps resolver;

    @Inject
    private WorkspaceOps workspaces;

    protected Cartographer()
    {
    }

    public Cartographer( final CartoDataManager database, final CalculationOps calculator, final GraphOps grapher, final GraphRenderingOps renderer,
                         final MetadataOps metadata, final ResolveOps resolver, final WorkspaceOps workspace )
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
