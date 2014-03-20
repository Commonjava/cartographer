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
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.NoneFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.util.JoinString;

//TODO: Find a way to store selections appropriately in depgraph. BUT, they have to be isolately appropriately to classloader...
public class BuildRequirementProjectsFilter
    implements ProjectRelationshipFilter
{

    private static final long serialVersionUID = 1L;

    private final boolean runtimeOnly;

    private final ProjectRelationshipFilter filter;

    private final Set<ProjectRef> excludes;

    private final boolean acceptManaged;

    private transient String longId;

    private transient String shortId;

    public BuildRequirementProjectsFilter()
    {
        this.runtimeOnly = false;
        this.acceptManaged = false;
        this.filter = null;
        this.excludes = null;
    }

    public BuildRequirementProjectsFilter( final boolean acceptManaged )
    {
        this.acceptManaged = acceptManaged;
        this.runtimeOnly = false;
        this.filter = null;
        this.excludes = null;
    }

    private BuildRequirementProjectsFilter( final boolean runtimeOnly, final boolean acceptManaged, final Set<ProjectRef> excludes )
    {
        //        logger.info( "Creating filter {}",
        //                     runtimeOnly ? "for runtime artifacts ONLY - only dependencies in the runtime/compile scope."
        //                                     : "for any artifact" );
        this.runtimeOnly = runtimeOnly;
        this.acceptManaged = acceptManaged;
        this.filter =
            runtimeOnly ? new OrFilter( new DependencyFilter( DependencyScope.runtime, ScopeTransitivity.maven, false, true, excludes ),
                                        new DependencyFilter( DependencyScope.embedded, ScopeTransitivity.maven, false, true, excludes ) ) : null;
        this.excludes = excludes;
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
            result = ( excludes == null || !excludes.contains( rel.getTarget()
                                                                  .asProjectRef() ) ) && ( filter == null || filter.accept( rel ) );
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
            {
                return new BuildRequirementProjectsFilter( true, false, excludes );
            }
            case PLUGIN_DEP:
            {
                //                logger.info( "getChildFilter({})", lastRelationship );

                // reset selections map to simulate classloader isolation.
                return new BuildRequirementProjectsFilter( true, false, excludes );
            }
            case PARENT:
            {
                return this;
            }
            default:
            {
                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;

                Set<ProjectRef> exc = null;
                boolean construct = false;

                // if there are new excludes, ALWAYS construct a new child filter.
                if ( dr.getExcludes() != null && !dr.getExcludes()
                                                    .isEmpty() )
                {
                    if ( excludes != null )
                    {
                        exc = new HashSet<ProjectRef>( excludes );
                    }

                    exc.addAll( dr.getExcludes() );
                    construct = true;
                }

                boolean nextRuntimeOnly = runtimeOnly;

                // runtimeOnly => runtime scope ...include only runtime ...return THIS
                // runtimeOnly => test scope  ...exclude all
                if ( runtimeOnly )
                {
                    if ( !DependencyScope.runtime.implies( dr.getScope() ) )
                    {
                        return NoneFilter.INSTANCE;
                    }
                    else
                    {
                        // defer to the excludes calculation above...
                    }
                }
                // !runtimeOnly => test scope ...include only runtime
                // !runtimeOnly => runtime scope ...include all ...return THIS
                else
                {
                    if ( !DependencyScope.runtime.implies( dr.getScope() ) )
                    {
                        nextRuntimeOnly = true;
                        construct = true;
                    }
                    else
                    {
                        // defer to excludes
                    }
                }

                if ( construct )
                {
                    return new BuildRequirementProjectsFilter( nextRuntimeOnly, acceptManaged && nextRuntimeOnly, exc );
                }

                return this;
            }
        }
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( acceptManaged ? 1231 : 1237 );
        result = prime * result + ( ( excludes == null ) ? 0 : excludes.hashCode() );
        result = prime * result + ( ( filter == null ) ? 0 : filter.hashCode() );
        result = prime * result + ( runtimeOnly ? 1231 : 1237 );
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
        final BuildRequirementProjectsFilter other = (BuildRequirementProjectsFilter) obj;
        if ( acceptManaged != other.acceptManaged )
        {
            return false;
        }
        if ( excludes == null )
        {
            if ( other.excludes != null )
            {
                return false;
            }
        }
        else if ( !excludes.equals( other.excludes ) )
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
        if ( runtimeOnly != other.runtimeOnly )
        {
            return false;
        }
        return true;
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "Build-Requires(sub-filter:" );

            if ( filter == null )
            {
                sb.append( "none" );
            }
            else
            {
                sb.append( filter.getLongId() );
            }

            sb.append( ",runtimeOnly:" )
              .append( runtimeOnly )
              .append( ",excludes:{" )
              .append( new JoinString( ",", excludes ) )
              .append( "},acceptManaged:" )
              .append( acceptManaged )
              .append( ")" );

            longId = sb.toString();
        }

        return longId;
    }

    @Override
    public String getCondensedId()
    {
        if ( shortId == null )
        {
            shortId = DigestUtils.shaHex( getLongId() );
        }

        return shortId;
    }

}
