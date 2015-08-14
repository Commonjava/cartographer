package org.commonjava.cartographer.result;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class MappedProjectRelationshipsResult
{
    private Set<MappedProjectRelationships> projects;

    public void addProject( MappedProjectRelationships project )
    {
        if ( projects == null )
        {
            projects = new HashSet<>();
        }

        projects.add( project );
    }

    public Set<MappedProjectRelationships> getProjects()
    {
        return projects;
    }

    public void setProjects( Set<MappedProjectRelationships> projects )
    {
        this.projects = projects;
    }
}
