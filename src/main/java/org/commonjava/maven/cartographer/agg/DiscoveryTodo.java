package org.commonjava.maven.cartographer.agg;

import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

final class DiscoveryTodo
{
    private final ProjectVersionRef ref;

    private Set<ProjectRelationshipFilter> filters;

    DiscoveryTodo( final ProjectVersionRef ref )
    {
        this.ref = ref;
    }

    public DiscoveryTodo( final ProjectVersionRef ref, final Set<ProjectRelationshipFilter> filters )
    {
        this.ref = ref;
        this.filters = filters;
    }

    ProjectVersionRef getRef()
    {
        return ref;
    }

    Set<ProjectRelationshipFilter> getFilters()
    {
        return filters;
    }

    void setFilters( final Set<ProjectRelationshipFilter> filters )
    {
        this.filters = filters;
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
        return String.format( "DiscoveryTodo [ref=%s, filters=%s]", ref, filters );
    }

}