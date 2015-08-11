package org.commonjava.maven.cartographer.result;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jdcasey on 8/7/15.
 */
public class MappedProjectsResult
{
    private Set<MappedProjects> projects;

    public boolean addProject( MappedProjects projectMapping )
    {
        if ( projects == null )
        {
            projects = new HashSet<>();
        }

        return projects.add( projectMapping );
    }

    public Set<MappedProjects> getProjects()
    {
        return projects;
    }

    public void setProjects( Set<MappedProjects> projects )
    {
        this.projects = projects;
    }
}

