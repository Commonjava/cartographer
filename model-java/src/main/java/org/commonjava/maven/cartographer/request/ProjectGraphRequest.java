package org.commonjava.maven.cartographer.request;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ProjectGraphRequest
    extends SingleGraphRequest
{

    protected String projectGavPattern;

    protected ProjectVersionRef project;

    public String getProjectGavPattern()
    {
        return projectGavPattern;
    }

    public void setProjectGavPattern( final String projectGavPattern )
    {
        this.projectGavPattern = projectGavPattern;
    }

    public ProjectVersionRef getProject()
    {
        return project;
    }

    public void setProject( final ProjectVersionRef project )
    {
        this.project = project;
    }

}
