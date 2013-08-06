package org.commonjava.maven.cartographer.dto;

import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.util.GraphUtils;

public class GraphDifference
{

    private final Set<ProjectVersionRef> fromProjects;

    private final ProjectRelationshipFilter fromFilter;

    private final Set<ProjectVersionRef> toProjects;

    private final ProjectRelationshipFilter toFilter;

    private final Set<ProjectRelationship<?>> added;

    private final Set<ProjectRelationship<?>> removed;

    public GraphDifference( final Set<ProjectVersionRef> fromProjects, final ProjectRelationshipFilter fromFilter,
                            final Set<ProjectVersionRef> toProjects, final ProjectRelationshipFilter toFilter,
                            final Set<ProjectRelationship<?>> added, final Set<ProjectRelationship<?>> removed )
    {
        this.fromProjects = fromProjects;
        this.fromFilter = fromFilter;
        this.toProjects = toProjects;
        this.toFilter = toFilter;
        this.added = added;
        this.removed = removed;
    }

    public Set<ProjectVersionRef> getFromProjects()
    {
        return fromProjects;
    }

    public ProjectRelationshipFilter getFromFilter()
    {
        return fromFilter;
    }

    public Set<ProjectVersionRef> getToProjects()
    {
        return toProjects;
    }

    public ProjectRelationshipFilter getToFilter()
    {
        return toFilter;
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

}
