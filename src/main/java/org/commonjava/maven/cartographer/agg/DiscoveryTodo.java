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
package org.commonjava.maven.cartographer.agg;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

final class DiscoveryTodo
{
    private final ProjectVersionRef ref;

    private Map<GraphPath<?>, GraphPathInfo> parentPaths;

    public DiscoveryTodo( final ProjectVersionRef ref )
    {
        this.ref = ref;
    }

    public DiscoveryTodo( final ProjectVersionRef ref, final GraphPath<?> path, final GraphPathInfo pathInfo )
    {
        this.ref = ref;
        this.parentPaths = new HashMap<GraphPath<?>, GraphPathInfo>();
        parentPaths.put( path, pathInfo );
    }

    public ProjectVersionRef getRef()
    {
        return ref;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( ref == null ) ? 0 : ref.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final DiscoveryTodo other = (DiscoveryTodo) obj;
        if ( ref == null )
        {
            if ( other.ref != null )
            {
                return false;
            }
        }
        else if ( !ref.equals( other.ref ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "DiscoveryTodo [ref=%s, parent-paths: %s]", ref, parentPaths );
    }

    public Map<GraphPath<?>, GraphPathInfo> getParentPathMap()
    {
        return parentPaths;
    }

    public void setParentPathMap( final Map<GraphPath<?>, GraphPathInfo> paths )
    {
        this.parentPaths = paths;
    }

    public void addParentPath( final GraphPath<?> path, final GraphPathInfo pathInfo )
    {
        if ( parentPaths == null )
        {
            parentPaths = new HashMap<GraphPath<?>, GraphPathInfo>();
        }

        parentPaths.put( path, pathInfo );
    }

}
