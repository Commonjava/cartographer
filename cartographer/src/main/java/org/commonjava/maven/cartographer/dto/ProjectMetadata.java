package org.commonjava.maven.cartographer.dto;

import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ProjectMetadata
{
    private ProjectVersionRef project;

    private Map<String, String> metadata;

    public ProjectMetadata()
    {
    }

    public ProjectMetadata( final ProjectVersionRef project )
    {
        this.project = project;
    }

    public ProjectMetadata( final ProjectVersionRef project, final Map<String, String> metadata )
    {
        this.project = project;
        this.metadata = metadata;
    }

    public ProjectVersionRef getProject()
    {
        return project;
    }

    public void setProject( final ProjectVersionRef project )
    {
        this.project = project;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public void setMetadata( final Map<String, String> metadata )
    {
        this.metadata = metadata;
    }

}
