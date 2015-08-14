package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.AbstractGraphRequest;

public interface GraphRequestOwner<T extends GraphRequestOwner<T, R>, R extends AbstractGraphRequest>
{

    T withGraphRequest( R recipe );

}
