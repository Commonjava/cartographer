package org.commonjava.maven.cartographer.result;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectErrors
{
    private List<ProjectError> projects;

    public synchronized void addProject( ProjectError error )
    {
        if ( projects == null )
        {
            projects = new ArrayList<>();
        }
        projects.add( error );
    }

    public List<ProjectError> getProjects()
    {
        return projects;
    }

    public void setProjects( List<ProjectError> projects )
    {
        this.projects = projects;
    }
}
