package org.commonjava.maven.cartographer.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;

@ApplicationScoped
@Named( "no-op" )
@Alternative
public class NoOpCartoEventManager
    implements CartoEventManager
{

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final CartoDataManager data, final long timeoutMillis )
        throws CartoDataException
    {
    }

    @Override
    public void fireErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
    }

    @Override
    public void fireStorageEvent( final RelationshipStorageEvent evt )
    {
    }

}
