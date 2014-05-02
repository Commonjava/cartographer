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
        funnel.fireStorageEvent( new RelationshipStorageEvent( relationships, rejected ) );
    }

    @Override
    public void projectError( final RelationshipGraph graph, final ProjectVersionRef ref,
                              final Throwable error )
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
