package org.commonjava.maven.cartographer.dto;

import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphExport
{

    private Set<ProjectRelationship<?>> relationships;

    private Set<ProjectVersionRef> missingProjects;

    private Set<ProjectVersionRef> variableProjects;

    private Map<ProjectVersionRef, Throwable> projectErrors;

    public GraphExport( final Set<ProjectRelationship<?>> relationships, final Set<ProjectVersionRef> missingProjects,
                        final Set<ProjectVersionRef> variableProjects,
                        final Map<ProjectVersionRef, Throwable> projectErrors )
    {
        this.relationships = relationships;
        this.missingProjects = missingProjects;
        this.variableProjects = variableProjects;
        this.projectErrors = projectErrors;
    }

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( final Set<ProjectRelationship<?>> relationships )
    {
        this.relationships = relationships;
    }

    public Set<ProjectVersionRef> getMissingProjects()
    {
        return missingProjects;
    }

    public void setMissingProjects( final Set<ProjectVersionRef> missingProjects )
    {
        this.missingProjects = missingProjects;
    }

    public Set<ProjectVersionRef> getVariableProjects()
    {
        return variableProjects;
    }

    public void setVariableProjects( final Set<ProjectVersionRef> variableProjects )
    {
        this.variableProjects = variableProjects;
    }

    public Map<ProjectVersionRef, Throwable> getProjectErrors()
    {
        return projectErrors;
    }

    public void setProjectErrors( final Map<ProjectVersionRef, Throwable> projectErrors )
    {
        this.projectErrors = projectErrors;
    }

}
