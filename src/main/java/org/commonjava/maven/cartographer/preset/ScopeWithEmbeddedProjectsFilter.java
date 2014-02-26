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
package org.commonjava.maven.cartographer.preset;

import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.NoneFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;

public class ScopeWithEmbeddedProjectsFilter
    implements ProjectRelationshipFilter
{

    private final ProjectRelationshipFilter filter;

    private final boolean acceptManaged;

    private DependencyScope scope;

    public ScopeWithEmbeddedProjectsFilter( final DependencyScope scope, final boolean acceptManaged )
    {
        this.scope = scope == null ? DependencyScope.runtime : scope;
        this.acceptManaged = acceptManaged;
        this.filter =
            new OrFilter( ParentFilter.EXCLUDE_TERMINAL_PARENTS,
                          new DependencyFilter( this.scope, ScopeTransitivity.maven, false, true, true, null ),
                          new DependencyFilter( DependencyScope.embedded, ScopeTransitivity.maven, false, true, true, null ) );
    }

    private ScopeWithEmbeddedProjectsFilter( final DependencyScope scope, final ProjectRelationshipFilter childFilter )
    {
        this.acceptManaged = false;
        this.filter =
            childFilter == null ? new OrFilter( ParentFilter.EXCLUDE_TERMINAL_PARENTS, new DependencyFilter( scope, ScopeTransitivity.maven, false,
                                                                                                             true, true, null ),
                                                new DependencyFilter( DependencyScope.embedded, ScopeTransitivity.maven, false, true, true, null ) )
                            : childFilter;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean result;
        if ( isBOM( rel ) )
        {
            result = true;
        }
        else if ( !acceptManaged && rel.isManaged() )
        {
            result = false;
        }
        else
        {
            result = filter.accept( rel );
        }

        //        logger.info( "{}: accept({})", Boolean.toString( result )
        //                                              .toUpperCase(), rel );

        return result;
    }

    private boolean isBOM( final ProjectRelationship<?> rel )
    {
        if ( !rel.isManaged() )
        {
            return false;
        }

        if ( !( rel instanceof DependencyRelationship ) )
        {
            return false;
        }

        final DependencyRelationship dr = (DependencyRelationship) rel;
        return ( dr.getScope() == DependencyScope._import && "pom".equals( dr.getTargetArtifact()
                                                                             .getType() ) );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> lastRelationship )
    {
        switch ( lastRelationship.getType() )
        {
            case EXTENSION:
            case PLUGIN:
            case PLUGIN_DEP:
            {
                if ( filter == NoneFilter.INSTANCE )
                {
                    return this;
                }

                //                logger.info( "getChildFilter({})", lastRelationship );
                return new ScopeWithEmbeddedProjectsFilter( scope, NoneFilter.INSTANCE );
            }
            case PARENT:
            {
                return this;
            }
            default:
            {
                //                logger.info( "getChildFilter({})", lastRelationship );

                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;
                if ( DependencyScope.test == dr.getScope() || DependencyScope.provided == dr.getScope() )
                {
                    if ( filter == NoneFilter.INSTANCE )
                    {
                        return this;
                    }

                    return new ScopeWithEmbeddedProjectsFilter( scope, NoneFilter.INSTANCE );
                }

                final ProjectRelationshipFilter nextFilter = filter.getChildFilter( lastRelationship );
                if ( nextFilter == filter )
                {
                    return this;
                }

                return new ScopeWithEmbeddedProjectsFilter( scope, nextFilter );
            }
        }
    }

    @Override
    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "Scope-Plus-Embedded Projects Filter (sub-filter=" );

        if ( filter == null )
        {
            sb.append( "NONE" );
        }
        else
        {
            filter.render( sb );
        }

        sb.append( " [acceptManaged=" )
          .append( acceptManaged )
          .append( "])" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( acceptManaged ? 1231 : 1237 );
        result = prime * result + ( ( filter == null ) ? 0 : filter.hashCode() );
        result = prime * result + ( ( scope == null ) ? 0 : scope.hashCode() );
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
        final ScopeWithEmbeddedProjectsFilter other = (ScopeWithEmbeddedProjectsFilter) obj;
        if ( acceptManaged != other.acceptManaged )
        {
            return false;
        }
        if ( filter == null )
        {
            if ( other.filter != null )
            {
                return false;
            }
        }
        else if ( !filter.equals( other.filter ) )
        {
            return false;
        }
        if ( scope != other.scope )
        {
            return false;
        }
        return true;
    }

}
