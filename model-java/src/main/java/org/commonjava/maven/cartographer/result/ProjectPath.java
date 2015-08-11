package org.commonjava.maven.cartographer.result;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

import java.util.List;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectPath
{
    private List<ProjectRelationship<?>> pathParts;

    public ProjectPath(){}

    public ProjectPath(List<ProjectRelationship<?>> path )
    {
        this.pathParts = path;
    }

    public List<ProjectRelationship<?>> getPathParts()
    {
        return pathParts;
    }

    public void setPathParts( List<ProjectRelationship<?>> pathParts )
    {
        this.pathParts = pathParts;
    }
}
