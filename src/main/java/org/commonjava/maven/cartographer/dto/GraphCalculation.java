package org.commonjava.maven.cartographer.dto;

import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.util.GraphUtils;

public class GraphCalculation
{

    private final Type operation;

    private final Set<ProjectVersionRef> firstProjects;

    private final ProjectRelationshipFilter firstFilter;

    private final Set<ProjectVersionRef> secondProjects;

    private final ProjectRelationshipFilter secondFilter;

    private final Set<ProjectRelationship<?>> result;

    public GraphCalculation( final Type operation, final Set<ProjectVersionRef> firstProjects,
                             final ProjectRelationshipFilter firstFilter, final Set<ProjectVersionRef> secondProjects,
                             final ProjectRelationshipFilter secondFilter, final Set<ProjectRelationship<?>> result )
    {
        this.operation = operation;
        this.firstProjects = firstProjects;
        this.firstFilter = firstFilter;
        this.secondProjects = secondProjects;
        this.secondFilter = secondFilter;
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

    public Set<ProjectVersionRef> getFirstProjects()
    {
        return firstProjects;
    }

    public ProjectRelationshipFilter getFirstFilter()
    {
        return firstFilter;
    }

    public Set<ProjectVersionRef> getSecondProjects()
    {
        return secondProjects;
    }

    public ProjectRelationshipFilter getSecondFilter()
    {
        return secondFilter;
    }

    public Set<ProjectRelationship<?>> getResultingRelationships()
    {
        return result;
    }

    public Set<ProjectVersionRef> getResultingProjects()
    {
        return GraphUtils.targets( result );
    }

}
