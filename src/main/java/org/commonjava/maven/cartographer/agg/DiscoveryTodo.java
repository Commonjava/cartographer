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

import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

final class DiscoveryTodo
{
    private final ProjectVersionRef ref;

    private Set<ProjectRelationshipFilter> filters;

    private Set<GraphMutator> mutators;

    DiscoveryTodo( final ProjectVersionRef ref )
    {
        this.ref = ref;
    }

    public DiscoveryTodo( final ProjectVersionRef ref, final Set<ProjectRelationshipFilter> filters, final Set<GraphMutator> mutators )
    {
        this.ref = ref;
        this.filters = filters;
        this.mutators = mutators;
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

    public void setMutators( final Set<GraphMutator> mutators )
    {
        this.mutators = mutators;
    }

    public Set<GraphMutator> getMutators()
    {
        return mutators;
    }

}
