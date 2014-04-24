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

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.targets;

import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphCalculation
{

    private Type operation;

    private Set<ProjectRelationship<?>> result;

    private Set<ProjectVersionRef> roots;

    private List<GraphDescription> graphs;

    protected GraphCalculation()
    {
    }

    public GraphCalculation( final Type operation, final List<GraphDescription> graphs,
                             final Set<ProjectVersionRef> roots, final Set<ProjectRelationship<?>> result )
    {
        this.operation = operation;
        this.graphs = graphs;
        this.roots = roots;
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

    public Set<ProjectVersionRef> getResultingRoots()
    {
        return roots;
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
        return String.format( "GraphCalculation [operation=%s, result=%s, graphs=%s]", operation, result, graphs );
    }

}
