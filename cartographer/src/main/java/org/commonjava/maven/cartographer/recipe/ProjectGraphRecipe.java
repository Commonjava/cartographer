package org.commonjava.maven.cartographer.recipe;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ProjectGraphRecipe
    extends SingleGraphResolverRecipe
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
