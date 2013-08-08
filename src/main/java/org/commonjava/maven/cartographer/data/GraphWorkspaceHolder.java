package org.commonjava.maven.cartographer.data;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;

@ApplicationScoped
public class GraphWorkspaceHolder
{

    private final InheritableThreadLocal<GraphWorkspace> workspace = new InheritableThreadLocal<GraphWorkspace>();

    public GraphWorkspace getCurrentWorkspace()
    {
        return workspace.get();
    }

    public void setCurrentWorkspace( final GraphWorkspace newWs, final boolean clearExisting )
        throws CartoDataException
    {
        if ( newWs == null )
        {
            return;
        }

        if ( workspace.get() != null && !clearExisting )
        {
            return;
        }

        clearCurrentWorkspace();
        workspace.set( newWs );
    }

    public void clearCurrentWorkspace()
    {
        final GraphWorkspace existing = workspace.get();
        if ( existing != null )
        {
            workspace.set( null );
        }
    }

}
