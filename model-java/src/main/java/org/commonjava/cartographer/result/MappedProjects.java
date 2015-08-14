package org.commonjava.cartographer.result;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.List;

/**
 * Created by jdcasey on 8/7/15.
 */
public class MappedProjects
{
    private ProjectVersionRef project;

    private List<ProjectVersionRef> mappedProjects;

    public MappedProjects(){}

    public MappedProjects(ProjectVersionRef project, List<ProjectVersionRef> mappedProjects )
    {
        this.project = project;
        this.mappedProjects = mappedProjects;
    }

    public ProjectVersionRef getProject()
    {
        return project;
    }

    public void setProject( ProjectVersionRef project )
    {
        this.project = project;
    }

    public List<ProjectVersionRef> getMappedProjects()
    {
        return mappedProjects;
    }

    public void setMappedProjects( List<ProjectVersionRef> mappedProjects )
    {
        this.mappedProjects = mappedProjects;
    }

}
