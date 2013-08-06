package org.commonjava.maven.cartographer.event;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;

public interface CartoEventManager
{

    void waitForGraph( ProjectVersionRef ref, long timeoutMillis )
        throws CartoDataException;

    void unlockOnRelationshipsErrorEvent( final ProjectRelationshipsErrorEvent evt );

    void unlockOnNewRelationshipsEvent( final NewRelationshipsEvent evt );

    void fireMissing( MissingRelationshipsEvent missingRelationshipsEvent );

}
