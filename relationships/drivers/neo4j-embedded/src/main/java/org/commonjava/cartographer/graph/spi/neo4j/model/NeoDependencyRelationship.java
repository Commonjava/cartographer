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
package org.commonjava.cartographer.graph.spi.neo4j.model;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.cartographer.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class NeoDependencyRelationship
    extends AbstractNeoProjectRelationship<NeoDependencyRelationship, DependencyRelationship, ArtifactRef>
    implements Serializable, DependencyRelationship
{

    private static final long serialVersionUID = 1L;

    private DependencyScope scope;

    public NeoDependencyRelationship( final Relationship rel )
    {
        super(rel, RelationshipType.DEPENDENCY);
    }

    @Override
    public final DependencyScope getScope()
    {
        if (scope == null)
        {
            final String scopeStr = Conversions.getStringProperty( Conversions.SCOPE, rel );
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug("Got scope from relationship: {} of: {}", rel, scopeStr );

            scope = DependencyScope.getScope( scopeStr );
            scope = scope == null ? DependencyScope.compile : scope;
        }
        return scope;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( isManaged() ? 1231 : 1237 );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( !( obj instanceof DependencyRelationship ) )
        {
            return false;
        }
        final DependencyRelationship other = (DependencyRelationship) obj;
        return isManaged() == other.isManaged();
    }

    @Override
    public DependencyRelationship detach()
    {
        return new SimpleDependencyRelationship( this );
    }

    @Override
    public String toString()
    {
        return String.format( "DependencyRelationship [%s => %s (managed=%s, scope=%s, index=%s)]", getDeclaring(),
                              getTarget(), isManaged(), getScope(), getIndex() );
    }

    @Override
    public ArtifactRef getTarget()
    {
        return target == null ? new NeoArtifactRef( rel.getEndNode(), new NeoTypeAndClassifier( rel ) ) : target;
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget();
    }

    @Override
    public Set<ProjectRef> getExcludes()
    {
        final String excludeStr = Conversions.getStringProperty( Conversions.EXCLUDES, rel );
        final Set<ProjectRef> excludes = new HashSet<ProjectRef>();
        if ( excludeStr != null )
        {
            final String[] e = excludeStr.split( "\\s*,\\s*" );
            for ( final String ex : e )
            {
                final String[] parts = ex.split( ":" );
                if ( parts.length != 2 )
                {
                    //                            LOGGER.error( "In: {} -> {} skipping invalid exclude specification: '{}'", from, artifact, ex );
                }
                else
                {
                    excludes.add( new NeoProjectRef( parts[0], parts[1] ) );
                }
            }
        }

        return excludes;
    }

    @Override
    public DependencyRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        return new NeoDependencyRelationship( rel ).cloneDirtyState( this ).withDeclaring( ref );
    }

    @Override
    public DependencyRelationship selectTarget( final ProjectVersionRef ref )
    {
        return new NeoDependencyRelationship( rel ).cloneDirtyState( this ).withTarget( NeoIdentityUtils.newNeoArtifactRef(
                ref, getTarget() ) );
    }

    @Override
    public DependencyRelationship addSource( final URI source )
    {
        Set<URI> sources = getSources();
        if ( sources.add( source ) )
        {
            return new NeoDependencyRelationship( rel ).cloneDirtyState( this ).withSources( sources );
        }

        return this;
    }

    @Override
    public DependencyRelationship addSources( final Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        boolean changed = false;
        for ( URI src: sources )
        {
            changed = srcs.add( src ) || changed;
        }

        if ( changed )
        {
            return new NeoDependencyRelationship( rel ).cloneDirtyState( this ).withSources( srcs );
        }

        return this;
    }

    @Override
    public boolean isBOM()
    {
        return DependencyScope._import == getScope() && "pom".equals( getTargetArtifact().getType() );
    }

    @Override
    public boolean isOptional()
    {
        return Conversions.getBooleanProperty( Conversions.OPTIONAL, rel );
    }

}
