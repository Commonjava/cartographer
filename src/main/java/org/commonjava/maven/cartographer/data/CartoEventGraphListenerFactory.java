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
package org.commonjava.maven.cartographer.data;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.AbstractRelationshipGraphListener;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphListenerFactory;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.event.CartoEventManager;
import org.commonjava.maven.cartographer.event.ProjectRelationshipsErrorEvent;
import org.commonjava.maven.cartographer.event.RelationshipStorageEvent;

@ApplicationScoped
public class CartoEventGraphListenerFactory
    extends AbstractRelationshipGraphListener
    implements RelationshipGraphListenerFactory
{

    @Inject
    private CartoEventManager funnel;

    @Override
    public void stored( final RelationshipGraph graph,
                        final Collection<? extends ProjectRelationship<?>> relationships,
                        final Collection<ProjectRelationship<?>> rejected )
        throws RelationshipGraphException
    {
        funnel.fireStorageEvent( new RelationshipStorageEvent( relationships, rejected, graph ) );
    }

    @Override
    public void projectError( final RelationshipGraph graph, final ProjectVersionRef ref, final Throwable error )
        throws RelationshipGraphException
    {
        funnel.fireErrorEvent( new ProjectRelationshipsErrorEvent( graph, ref, error ) );
    }

    @Override
    public void addListeners( final RelationshipGraph graph )
    {
        graph.addListener( this );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( funnel == null ) ? 0 : funnel.hashCode() );
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
        final CartoEventGraphListenerFactory other = (CartoEventGraphListenerFactory) obj;
        if ( funnel == null )
        {
            if ( other.funnel != null )
            {
                return false;
            }
        }
        else if ( !funnel.equals( other.funnel ) )
        {
            return false;
        }
        return true;
    }

}
