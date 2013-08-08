package org.commonjava.maven.cartographer.event;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;

public interface CartoEventManager
{

    void waitForGraph( ProjectVersionRef ref, CartoDataManager data, long timeoutMillis )
        throws CartoDataException;

    void unlockOnRelationshipsErrorEvent( final ProjectRelationshipsErrorEvent evt );

    void unlockOnNewRelationshipsEvent( final NewRelationshipsEvent evt );

    void fireMissing( MissingRelationshipsEvent missingRelationshipsEvent );

}
