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
package org.commonjava.maven.cartographer.dto;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.targets;

import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphCalculation
{

    private Type operation;

    private Set<ProjectRelationship<?>> result;

    private List<GraphDescription> graphs;

    protected GraphCalculation()
    {
    }

    public GraphCalculation( final Type operation, final List<GraphDescription> graphs, final Set<ProjectRelationship<?>> result )
    {
        this.operation = operation;
        this.graphs = graphs;
        this.result = result;
    }

    public enum Type
    {
        ADD, SUBTRACT, INTERSECT;
    }

    public Type getOperation()
    {
        return operation;
    }

    public Set<ProjectRelationship<?>> getResultingRelationships()
    {
        return result;
    }

    public Set<ProjectVersionRef> getResultingProjects()
    {
        return targets( result );
    }

    public Set<ProjectRelationship<?>> getResult()
    {
        return result;
    }

    public List<GraphDescription> getGraphs()
    {
        return graphs;
    }

    protected void setOperation( final Type operation )
    {
        this.operation = operation;
    }

    protected void setResult( final Set<ProjectRelationship<?>> result )
    {
        this.result = result;
    }

    protected void setGraphs( final List<GraphDescription> graphs )
    {
        this.graphs = graphs;
    }

    @Override
    public String toString()
    {
        return String.format( "GraphCalculation [operation={}, result={}, graphs={}]", operation, result, graphs );
    }

}
