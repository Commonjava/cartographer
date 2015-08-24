/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.graph.preset;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.BOM;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.filter.StructuralRelationshipsFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.util.JoinString;

//TODO: Find a way to store selections appropriately in depgraph. BUT, they have to be isolately appropriately to classloader...
public class BuildRequirementProjectsFilter
    implements ProjectRelationshipFilter
{

    private static final long serialVersionUID = 1L;

    private final Set<ProjectRef> excludes;

    private final boolean acceptManaged;

    private transient String longId;

    private transient String shortId;

    public BuildRequirementProjectsFilter()
    {
        this( false );
    }

    public BuildRequirementProjectsFilter( final boolean acceptManaged )
    {
        this( acceptManaged, null );
    }

    public BuildRequirementProjectsFilter( final boolean acceptManaged , Set<ProjectRef> excludes  )
    {
        this.acceptManaged = acceptManaged;
        this.excludes = excludes;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean result = false;

        if ( rel.getType() == PARENT )
        {
            result = true;
        }
        else if ( rel.getType() == BOM )
        {
            result = true;
        }
        else if ( !acceptManaged && rel.isManaged() )
        {
            result = false;
        }
        else if ( rel.getType() == DEPENDENCY )
        {
            result =
                ( excludes == null || !excludes.contains( rel.getTarget()
                                                             .asProjectRef() ) );
        }
        else
        {
            result = true;
        }

        //        logger.info( "{}: accept({})", Boolean.toString( result )
        //                                              .toUpperCase(), rel );

        return result;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> lastRelationship )
    {
        switch ( lastRelationship.getType() )
        {
            case BOM:
                return StructuralRelationshipsFilter.INSTANCE;
            case PARENT:
            {
                return this;
            }
            case EXTENSION:
            case PLUGIN:
            case PLUGIN_DEP:
            {
                return new ScopeWithEmbeddedProjectsFilter( DependencyScope.runtime, acceptManaged );
            }
            default:
            {
                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;

                Set<ProjectRef> exc = null;

                // if there are new excludes, ALWAYS construct a new child filter.
                if ( dr.getExcludes() != null && !dr.getExcludes()
                                                    .isEmpty() )
                {
                    if ( excludes != null )
                    {
                        exc = new HashSet<ProjectRef>( excludes );
                        exc.addAll( dr.getExcludes() );
                    }
                    else
                    {
                        exc = new HashSet<ProjectRef>( dr.getExcludes() );
                    }
                }
                else if ( excludes != null )
                {
                    exc = new HashSet<ProjectRef>( excludes );
                }

                return new ScopeWithEmbeddedProjectsFilter( DependencyScope.runtime, acceptManaged, exc );
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
        return true;
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "Build-Requires(" );

            sb.append( "excludes:{" )
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

    @Override
    public boolean includeManagedRelationships()
    {
        return acceptManaged;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        return true;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        final Set<RelationshipType> types = new HashSet<>();
        types.addAll( Arrays.asList( RelationshipType.values() ) );

        return types;
    }

}
