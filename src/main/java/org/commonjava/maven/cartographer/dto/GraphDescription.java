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
package org.commonjava.maven.cartographer.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

// TODO: Allow configuration of mutator too...
public class GraphDescription
{

    private Set<ProjectVersionRef> roots;

    private String preset;

    private Map<String, Object> presetParams = new TreeMap<String, Object>();

    private transient ProjectRelationshipFilter filter;

    private transient ViewParams view;

    protected GraphDescription()
    {
    }

    public GraphDescription( final String preset, final Map<String, Object> presetParams,
                             final Collection<ProjectVersionRef> roots )
    {
        this.preset = preset;
        this.presetParams = new TreeMap<>( presetParams );
        this.roots = new TreeSet<ProjectVersionRef>( roots );
    }

    public GraphDescription( final String preset, final Map<String, Object> presetParams,
                             final ProjectVersionRef... roots )
    {
        this( preset, presetParams, Arrays.asList( roots ) );
    }

    public GraphDescription( final ProjectRelationshipFilter filter, final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.roots = new TreeSet<ProjectVersionRef>( roots );
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
        this.roots = new TreeSet<>( roots );
    }

    public void setPreset( final String preset )
    {
        this.preset = preset;
    }

    public ProjectVersionRef[] rootsArray()
    {
        return roots == null ? new ProjectVersionRef[0] : roots.toArray( new ProjectVersionRef[roots.size()] );
    }

    public ProjectRelationshipFilter filter()
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

    public Map<String, Object> getPresetParams()
    {
        return presetParams;
    }

    public void setGraphParams( final ViewParams view )
    {
        this.view = view;
        this.roots = new TreeSet<>( view.getRoots() );
    }

    public ViewParams view()
    {
        return view;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( preset == null ) ? 0 : preset.hashCode() );
        result = prime * result + ( ( presetParams == null ) ? 0 : presetParams.hashCode() );
        result = prime * result + ( ( roots == null ) ? 0 : roots.hashCode() );
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
        final GraphDescription other = (GraphDescription) obj;
        if ( preset == null )
        {
            if ( other.preset != null )
            {
                return false;
            }
        }
        else if ( !preset.equals( other.preset ) )
        {
            return false;
        }
        if ( presetParams == null )
        {
            if ( other.presetParams != null )
            {
                return false;
            }
        }
        else if ( !presetParams.equals( other.presetParams ) )
        {
            return false;
        }
        if ( roots == null )
        {
            if ( other.roots != null )
            {
                return false;
            }
        }
        else if ( !roots.equals( other.roots ) )
        {
            return false;
        }
        return true;
    }

}
