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
package org.commonjava.maven.cartographer.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphDescription
{

    private Set<ProjectVersionRef> roots;

    private String preset;

    private transient ProjectRelationshipFilter filter;

    protected GraphDescription()
    {
    }

    public GraphDescription( final String preset, final Collection<ProjectVersionRef> roots )
    {
        this.preset = preset;
        this.roots = new HashSet<ProjectVersionRef>( roots );
    }

    public GraphDescription( final String preset, final ProjectVersionRef... roots )
    {
        this( preset, Arrays.asList( roots ) );
    }

    public GraphDescription( final ProjectRelationshipFilter filter, final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.roots = new HashSet<ProjectVersionRef>( roots );
    }

    public GraphDescription( final ProjectRelationshipFilter filter, final ProjectVersionRef... roots )
    {
        this( filter, Arrays.asList( roots ) );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public String getPreset()
    {
        return preset;
    }

    public void setRoots( final Set<ProjectVersionRef> roots )
    {
        this.roots = roots;
    }

    public void setPreset( final String preset )
    {
        this.preset = preset;
    }

    public ProjectVersionRef[] getRootsArray()
    {
        return roots == null ? new ProjectVersionRef[0] : roots.toArray( new ProjectVersionRef[roots.size()] );
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public void setFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
    }

    @Override
    public String toString()
    {
        return String.format( "GraphDescription [roots=%s, preset=%s, filter=%s]", roots, preset, filter );
    }

    public void normalize()
    {
        for ( final Iterator<ProjectVersionRef> it = roots.iterator(); it.hasNext(); )
        {
            if ( it.next() == null )
            {
                it.remove();
            }
        }
    }

}
