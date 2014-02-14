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
            new OrFilter( new ParentFilter( false ), new DependencyFilter( this.scope, ScopeTransitivity.maven, false, true, true, null ),
                          new DependencyFilter( DependencyScope.embedded, ScopeTransitivity.maven, false, true, true, null ) );
    }

    private ScopeWithEmbeddedProjectsFilter( final DependencyScope scope, final ProjectRelationshipFilter childFilter )
    {
        this.acceptManaged = false;
        this.filter =
            childFilter == null ? new OrFilter( new ParentFilter( false ), new DependencyFilter( scope, ScopeTransitivity.maven, false, true, true,
                                                                                                 null ),
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

        //        logger.info( "%s: accept(%s)", Boolean.toString( result )
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
                //                logger.info( "getChildFilter(%s)", lastRelationship );
                return new ScopeWithEmbeddedProjectsFilter( scope, new NoneFilter() );
            }
            case PARENT:
            {
                return this;
            }
            default:
            {
                //                logger.info( "getChildFilter(%s)", lastRelationship );

                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;
                if ( DependencyScope.test == dr.getScope() || DependencyScope.provided == dr.getScope() )
                {
                    return new ScopeWithEmbeddedProjectsFilter( scope, new NoneFilter() );
                }

                return new ScopeWithEmbeddedProjectsFilter( scope, filter.getChildFilter( lastRelationship ) );
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
        sb.append( "Shipping-Oriented Builds Filter (sub-filter=" );

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

}
