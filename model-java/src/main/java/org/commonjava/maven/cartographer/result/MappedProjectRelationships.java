package org.commonjava.maven.cartographer.result;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class MappedProjectRelationships
{

    private ProjectVersionRef project;

    private Set<ProjectRelationship<?>> relationships;

    public MappedProjectRelationships()
    {
    }

    public MappedProjectRelationships( ProjectVersionRef project, Set<ProjectRelationship<?>> relationships )
    {
        this.project = project;
        this.relationships = relationships;

    }

    public ProjectVersionRef getProject()
    {
        return project;
    }

    public void setProject( ProjectVersionRef project )
    {
        this.project = project;
    }

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( Set<ProjectRelationship<?>> relationships )
    {
        this.relationships = relationships;
    }
}
