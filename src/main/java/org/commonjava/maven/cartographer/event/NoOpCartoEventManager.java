package org.commonjava.maven.cartographer.event;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;

@ApplicationScoped
@Named( "no-op" )
public class NoOpCartoEventManager
    implements CartoEventManager
{

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final CartoDataManager data, final long timeoutMillis )
        throws CartoDataException
    {
    }

    @Override
    public void unlockOnRelationshipsErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
    }

    @Override
    public void unlockOnNewRelationshipsEvent( final NewRelationshipsEvent evt )
    {
    }

    @Override
    public void fireMissing( final MissingRelationshipsEvent missingRelationshipsEvent )
    {
    }

}
