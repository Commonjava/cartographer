package org.commonjava.cartographer.result;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectListResult
{
    private List<ProjectVersionRef> projects;

    public synchronized void addProject( ProjectVersionRef ref )
    {
        if ( projects == null )
        {
            projects = new ArrayList<>();
        }

        projects.add(ref);
    }

    public List<ProjectVersionRef> getProjects()
    {
        return projects;
    }

    public void setProjects( List<ProjectVersionRef> projects )
    {
        this.projects = projects;
    }
}
