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

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

// TODO: Allow configuration of mutator too...
public class GraphDescription
{

    private Set<ProjectVersionRef> roots;

    private String preset;

    private Map<String, Object> presetParams;

    private transient ProjectRelationshipFilter filter;

    private transient GraphView view;

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

    public Map<String, Object> getPresetParams()
    {
        return presetParams;
    }

    public void setView( final GraphView view )
    {
        this.view = view;
        this.roots = new TreeSet<>( view.getRoots() );
    }

    public GraphView getView()
    {
        return view;
    }

}
