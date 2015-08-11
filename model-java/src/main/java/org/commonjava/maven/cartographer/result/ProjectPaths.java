package org.commonjava.maven.cartographer.result;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectPaths
{
    private List<ProjectPath> paths;

    public synchronized boolean addPath( ProjectPath path )
    {
        if ( paths == null )
        {
            paths = new ArrayList<>();
        }

        return paths.add( path );
    }

    public List<ProjectPath> getPaths()
    {
        return paths;
    }

    public void setPaths( List<ProjectPath> paths )
    {
        this.paths = paths;
    }
}
