package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphDescription;

public interface GraphDescriptionOwner<T extends GraphDescriptionOwner<T>>
{

    T withGraph( GraphDescription graph );

}
