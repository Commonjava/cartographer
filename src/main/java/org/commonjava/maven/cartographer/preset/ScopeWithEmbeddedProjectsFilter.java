/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.cartographer.preset;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.BOM;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.NoneFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.filter.StructuralRelationshipsFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopeWithEmbeddedProjectsFilter
    implements ProjectRelationshipFilter
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final long serialVersionUID = 1L;

    private final ProjectRelationshipFilter filter;

    private final Set<ProjectRef> excludes;

    private final boolean acceptManaged;

    private transient String longId;

    private transient String shortId;

    public ScopeWithEmbeddedProjectsFilter( final DependencyScope scope, final boolean acceptManaged )
    {
        this( scope, acceptManaged, null );
    }
    
    ScopeWithEmbeddedProjectsFilter( final DependencyScope scope, final boolean acceptManaged, Set<ProjectRef> exc )
    {
        DependencyScope filterScope = scope == null ? DependencyScope.runtime : scope;
        this.acceptManaged = acceptManaged;
        this.filter =
            new OrFilter( new DependencyFilter( filterScope, ScopeTransitivity.maven, false, true, true, null ),
                          new DependencyFilter( DependencyScope.embedded, ScopeTransitivity.maven, false, true, true,
                                                null ) );
        this.excludes = exc;
    }

    ScopeWithEmbeddedProjectsFilter( final ProjectRelationshipFilter childFilter, final boolean acceptManaged,
                                     final Set<ProjectRef> excludes )
    {
        this.acceptManaged = acceptManaged;
        this.filter = childFilter;
        this.excludes = excludes;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean result;
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
            result = ( excludes == null || !excludes.contains( rel.getTarget().asProjectRef() ) )
                    && filter.accept( rel );
        }
        else
        {
            result = filter.accept( rel );
        }

        logger.debug( "{} {}", ( result ? "IN" : "OUT" ), rel );

        //        if ( !result )
        //        {
        //            logger.info( "[FILT-STOP]: {}", rel );
        //        }

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
                logger.debug( "[FILT-OFFx1]: {}", lastRelationship );
                return NoneFilter.INSTANCE;
                //                if ( filter == NoneFilter.INSTANCE )
                //                {
                //                    return this;
                //                }
                //
                //                //                logger.info( "getChildFilter({})", lastRelationship );
                //                return new ScopeWithEmbeddedProjectsFilter( scope, NoneFilter.INSTANCE );
            }
            default:
            {
                //                logger.info( "getChildFilter({})", lastRelationship );

                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;

                if ( DependencyScope.test == dr.getScope() || DependencyScope.provided == dr.getScope() )
                {
                    logger.debug( "[FILT-OFFx2]: {}", lastRelationship );
                    return StructuralRelationshipsFilter.INSTANCE;
                }
                
                Set<ProjectRef> exc = null;
                boolean excChanged = false;
                
                // if there are new excludes, ALWAYS construct a new child filter.
                if ( dr.getExcludes() != null && !dr.getExcludes().isEmpty() )
                {
                    if ( excludes != null )
                    {
                        exc = new HashSet<ProjectRef>( excludes );
                        exc.addAll( dr.getExcludes() );
                        excChanged = !exc.equals( excludes );
                    }
                    else
                    {
                        exc = new HashSet<ProjectRef>( dr.getExcludes() );
                        excChanged = true;
                    }
                }
                else if ( excludes != null )
                {
                    exc = new HashSet<ProjectRef>( excludes );
                }

                final ProjectRelationshipFilter nextFilter = filter.getChildFilter( lastRelationship );
                if ( filter.equals( nextFilter ) && !excChanged )
                {
                    return this;
                }
                else
                {
                    return new ScopeWithEmbeddedProjectsFilter( nextFilter, acceptManaged, exc );
                }
            }
        }
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "Scope-Plus-Embedded(sub-filter:" );

            if ( filter == null )
            {
                sb.append( "none" );
            }
            else
            {
                sb.append( filter.getLongId() );
            }

            sb.append( ",excludes:{" )
              .append( new JoinString( ",", excludes ) )
              .append( "},acceptManaged:" )
              .append( acceptManaged )
              .append( ")" );

            longId = sb.toString();
        }

        return longId;
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
        result = prime * result + filter.hashCode();
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
        if ( !filter.equals( other.filter ) )
        {
            return false;
        }
        return true;
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
        types.add( PARENT );
        types.add( BOM );

        types.addAll( filter.getAllowedTypes() );

        return types;
    }

}
