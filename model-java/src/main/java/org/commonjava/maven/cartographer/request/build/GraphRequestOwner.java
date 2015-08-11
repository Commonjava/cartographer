package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.AbstractGraphRequest;

public interface GraphRequestOwner<T extends GraphRequestOwner<T, R>, R extends AbstractGraphRequest>
{

    T withGraphRequest( R recipe );

}
