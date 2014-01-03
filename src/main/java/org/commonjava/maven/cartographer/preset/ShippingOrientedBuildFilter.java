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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;

//TODO: Find a way to store selections appropriately in depgraph. BUT, they have to be isolately appropriately to classloader...
public class ShippingOrientedBuildFilter
    implements ProjectRelationshipFilter
{

    private final boolean runtimeOnly;

    private final ProjectRelationshipFilter filter;

    private final Set<ProjectRef> excludes = new HashSet<ProjectRef>();

    private final boolean acceptManaged;

    public ShippingOrientedBuildFilter()
    {
        this.runtimeOnly = false;
        this.acceptManaged = false;
        this.filter = null;
    }

    public ShippingOrientedBuildFilter( final boolean acceptManaged )
    {
        this.acceptManaged = acceptManaged;
        this.runtimeOnly = false;
        this.filter = null;
    }

    private ShippingOrientedBuildFilter( final boolean runtimeOnly, final boolean acceptManaged, final Set<ProjectRef> excludes )
    {
        //        logger.info( "Creating filter %s",
        //                     runtimeOnly ? "for runtime artifacts ONLY - only dependencies in the runtime/compile scope."
        //                                     : "for any artifact" );
        this.runtimeOnly = runtimeOnly;
        this.acceptManaged = acceptManaged;
        this.filter =
            runtimeOnly ? new OrFilter( new DependencyFilter( DependencyScope.runtime, ScopeTransitivity.maven, false, true, excludes ),
                                        new DependencyFilter( DependencyScope.embedded, ScopeTransitivity.maven, false, true, excludes ) ) : null;
        this.excludes.addAll( excludes );
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean result = false;

        if ( isBOM( rel ) )
        {
            result = true;
        }
        else if ( !acceptManaged && rel.isManaged() )
        {
            result = false;
        }
        else if ( rel.getType() == RelationshipType.PARENT )
        {
            result = true;
        }
        else
        {
            result = !excludes.contains( rel.getTarget()
                                            .asProjectRef() ) && ( filter == null || filter.accept( rel ) );
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
            {
                return new ShippingOrientedBuildFilter( true, false, excludes );
            }
            case PLUGIN_DEP:
            {
                //                logger.info( "getChildFilter(%s)", lastRelationship );

                // reset selections map to simulate classloader isolation.
                return new ShippingOrientedBuildFilter( true, false, excludes );
            }
            case PARENT:
            {
                return this;
            }
            default:
            {
                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;

                final Set<ProjectRef> exc = new HashSet<ProjectRef>();
                exc.addAll( excludes );
                if ( dr.getExcludes() != null )
                {
                    exc.addAll( dr.getExcludes() );
                }

                //                logger.info( "getChildFilter(%s)", lastRelationship );

                // As long as the scope is runtime or compile, this is still in the train to be rebuilt.
                // Otherwise, it's test or provided scope, and it's a build-requires situation...we don't need to rebuild it.
                return new ShippingOrientedBuildFilter( !DependencyScope.runtime.implies( dr.getScope() ), acceptManaged
                    && DependencyScope.runtime.implies( dr.getScope() ), exc );
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

        sb.append( "; runtimeOnly=" )
          .append( runtimeOnly )
          .append( "; excludes=[" );
        for ( final Iterator<ProjectRef> iterator = excludes.iterator(); iterator.hasNext(); )
        {
            final ProjectRef exclude = iterator.next();
            sb.append( exclude );
            if ( iterator.hasNext() )
            {
                sb.append( ", " );
            }
        }
        sb.append( "; acceptManaged=" )
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
