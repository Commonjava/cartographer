/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.data;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;

@ApplicationScoped
public class GraphWorkspaceHolder
{

    private InheritableThreadLocal<GraphWorkspace> workspace;

    public GraphWorkspace getCurrentWorkspace()
    {
        initTL();
        return workspace.get();
    }

    private synchronized void initTL()
    {
        synchronized ( this )
        {
            if ( workspace == null )
            {
                workspace = new InheritableThreadLocal<GraphWorkspace>();
            }
        }
    }

    public void setCurrentWorkspace( final GraphWorkspace newWs, final boolean clearExisting )
        throws CartoDataException
    {
        if ( newWs == null )
        {
            return;
        }

        initTL();
        if ( workspace.get() != null && !clearExisting )
        {
            return;
        }

        clearCurrentWorkspace();
        workspace.set( newWs );
    }

    public void clearCurrentWorkspace()
    {
        if ( workspace == null )
        {
            return;
        }

        final GraphWorkspace existing = workspace.get();
        if ( existing != null )
        {
            workspace.set( null );
        }
    }

}
