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
        this.roots = new HashSet<>( roots );
    }

    public GraphDescription( final String preset, final ProjectVersionRef... roots )
    {
        this( preset, Arrays.asList( roots ) );
    }

    public GraphDescription( final ProjectRelationshipFilter filter, final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.roots = new HashSet<>( roots );
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
