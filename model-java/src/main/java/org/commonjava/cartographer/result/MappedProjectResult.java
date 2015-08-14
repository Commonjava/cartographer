package org.commonjava.cartographer.result;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 8/7/15.
 */
public class MappedProjectResult
{
    private Map<ProjectVersionRef, ProjectVersionRef> projects;

    public void addProject( ProjectVersionRef from, ProjectVersionRef to )
    {
        if ( projects == null )
        {
            projects = new HashMap<>();
        }

        projects.put( from, to );
    }

    public Map<ProjectVersionRef, ProjectVersionRef> getProjects()
    {
        return projects;
    }

    public void setProjects( Map<ProjectVersionRef, ProjectVersionRef> projects )
    {
        this.projects = projects;
    }
}

