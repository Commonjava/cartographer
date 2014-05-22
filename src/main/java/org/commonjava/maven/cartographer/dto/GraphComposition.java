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
package org.commonjava.maven.cartographer.dto;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
import org.commonjava.maven.cartographer.preset.PresetSelector;

public class GraphComposition
    implements Iterable<GraphDescription>
{

    private GraphCalculation.Type calculation;

    private List<GraphDescription> graphs;

    protected GraphComposition()
    {
    }

    public GraphComposition( final Type calculation, final List<GraphDescription> graphs )
    {
        this.calculation = calculation;
        this.graphs = graphs;
    }

    public GraphCalculation.Type getCalculation()
    {
        return calculation;
    }

    public List<GraphDescription> getGraphs()
    {
        return graphs;
    }

    public void setCalculation( final GraphCalculation.Type calculation )
    {
        this.calculation = calculation;
    }

    public void setGraphs( final List<GraphDescription> graphs )
    {
        this.graphs = graphs;
    }

    public void resolveFilters( final PresetSelector presets, final String defaultPreset )
    {
        for ( final GraphDescription graph : getGraphs() )
        {
            if ( graph.getFilter() == null )
            {
                final ProjectRelationshipFilter filter =
                    presets.getPresetFilter( graph.getPreset(), defaultPreset, graph.getPresetParams() );
                graph.setFilter( filter );
            }
        }
    }

    public boolean isEmpty()
    {
        return graphs == null || graphs.isEmpty();
    }

    @Override
    public String toString()
    {
        return String.format( "GraphComposition [graphs=%s, calculation=%s]", graphs, calculation );
    }

    public void normalize()
    {
        for ( final Iterator<GraphDescription> it = graphs.iterator(); it.hasNext(); )
        {
            final GraphDescription graph = it.next();
            if ( graph == null )
            {
                it.remove();
                continue;
            }

            graph.normalize();
        }
    }

    @Override
    public Iterator<GraphDescription> iterator()
    {
        return graphs == null ? Collections.<GraphDescription> emptySet()
                                           .iterator() : graphs.iterator();
    }

    public boolean isValid()
    {
        return graphs != null && !graphs.isEmpty();
    }

    public int size()
    {
        return graphs == null ? 0 : graphs.size();
    }
}
