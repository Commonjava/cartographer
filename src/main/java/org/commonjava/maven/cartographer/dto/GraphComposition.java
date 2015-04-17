/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.cartographer.dto;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
import org.commonjava.maven.cartographer.preset.PresetSelector;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
            if ( graph.filter() == null )
            {
                final ProjectRelationshipFilter filter =
                    presets.getPresetFilter( graph.getPreset(), defaultPreset, graph.getPresetParams() );
                graph.setFilter( filter );
            }
        }
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

    @JsonIgnore
    public boolean valid()
    {
        return graphs != null && !graphs.isEmpty();
    }

    public int size()
    {
        return graphs == null ? 0 : graphs.size();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( calculation == null ) ? 0 : calculation.hashCode() );
        result = prime * result + ( ( graphs == null ) ? 0 : graphs.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final GraphComposition other = (GraphComposition) obj;
        if ( calculation != other.calculation )
        {
            return false;
        }
        if ( graphs == null )
        {
            if ( other.graphs != null )
            {
                return false;
            }
        }
        else if ( !graphs.equals( other.graphs ) )
        {
            return false;
        }
        return true;
    }
}
