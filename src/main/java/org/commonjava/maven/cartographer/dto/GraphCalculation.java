package org.commonjava.maven.cartographer.dto;

import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.util.GraphUtils;

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
        ADDITION, SUBTRACTION, INTERSECTION;
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
        return GraphUtils.targets( result );
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
