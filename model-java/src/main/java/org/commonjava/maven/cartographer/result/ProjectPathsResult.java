package org.commonjava.maven.cartographer.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectPathsResult
{
    private Map<ProjectVersionRef, ProjectPaths> projects;

    public Map<ProjectVersionRef, ProjectPaths> getProjects()
    {
        return projects;
    }

    public void setProjects( Map<ProjectVersionRef, ProjectPaths> projects )
    {
        this.projects = projects;
    }

    public boolean addPath( ProjectVersionRef ref, ProjectPath path ){
        if ( projects == null )
        {
            projects = new HashMap<>();
        }

        ProjectPaths projectPaths = projects.get( ref );

        if ( projectPaths == null ){
            projectPaths = new ProjectPaths();
            projects.put( ref, projectPaths );
        }

        return projectPaths.addPath( path );
    }
}
