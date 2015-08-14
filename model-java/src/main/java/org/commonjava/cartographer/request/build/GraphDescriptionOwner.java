package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphDescription;

public interface GraphDescriptionOwner<T extends GraphDescriptionOwner<T>>
{

    T withGraph( GraphDescription graph );

}
