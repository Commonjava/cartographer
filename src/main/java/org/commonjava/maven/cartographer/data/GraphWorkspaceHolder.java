/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
