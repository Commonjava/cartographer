package org.commonjava.maven.cartographer.dto;

import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.util.GraphUtils;

public class GraphDifference
{

    private Set<ProjectRelationship<?>> added;

    private Set<ProjectRelationship<?>> removed;

    private GraphDescription toGraph;

    private GraphDescription fromGraph;

    protected GraphDifference()
    {
    }

    public GraphDifference( final GraphDescription fromGraph, final GraphDescription toGraph, final Set<ProjectRelationship<?>> added,
                            final Set<ProjectRelationship<?>> removed )
    {
        this.fromGraph = fromGraph;
        this.toGraph = toGraph;
        this.added = added;
        this.removed = removed;
    }

    public Set<ProjectVersionRef> getFromProjects()
    {
        return fromGraph.getRoots();
    }

    public ProjectRelationshipFilter getFromFilter()
    {
        return fromGraph.getFilter();
    }

    public Set<ProjectVersionRef> getToProjects()
    {
        return toGraph.getRoots();
    }

    public ProjectRelationshipFilter getToFilter()
    {
        return toGraph.getFilter();
    }

    public Set<ProjectRelationship<?>> getAddedRelationships()
    {
        return added;
    }

    public Set<ProjectRelationship<?>> getRemovedRelationships()
    {
        return removed;
    }

    public Set<ProjectVersionRef> getAddedProjects()
    {
        return GraphUtils.targets( added );
    }

    public Set<ProjectVersionRef> getRemovedProjects()
    {
        return GraphUtils.targets( removed );
    }

    public Set<ProjectRelationship<?>> getAdded()
    {
        return added;
    }

    public Set<ProjectRelationship<?>> getRemoved()
    {
        return removed;
    }

    public GraphDescription getToGraph()
    {
        return toGraph;
    }

    public GraphDescription getFromGraph()
    {
        return fromGraph;
    }

    protected void setAdded( final Set<ProjectRelationship<?>> added )
    {
        this.added = added;
    }

    protected void setRemoved( final Set<ProjectRelationship<?>> removed )
    {
        this.removed = removed;
    }

    protected void setToGraph( final GraphDescription toGraph )
    {
        this.toGraph = toGraph;
    }

    protected void setFromGraph( final GraphDescription fromGraph )
    {
        this.fromGraph = fromGraph;
    }

}
