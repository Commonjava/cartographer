package org.commonjava.cartographer.result;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ProjectVersionRefComparator;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectError
    implements Comparable<ProjectError>
{
    private ProjectVersionRef project;
    private String error;

    public ProjectError(){}

    public ProjectError( ProjectVersionRef project, String error )
    {
        this.project = project;
        this.error = error;
    }

    public String getError()
    {
        return error;
    }

    public void setError( String error )
    {
        this.error = error;
    }

    public ProjectVersionRef getProject()
    {

        return project;
    }

    public void setProject( ProjectVersionRef project )
    {
        this.project = project;
    }

    @Override
    public int compareTo( ProjectError o )
    {
        return new ProjectVersionRefComparator().compare( project, o.getProject() );
    }
}
