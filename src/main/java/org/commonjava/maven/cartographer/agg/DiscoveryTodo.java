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
        return String.format( "DiscoveryTodo [ref=%s, filters=%s, paths: %s]", ref, parentPaths );
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
