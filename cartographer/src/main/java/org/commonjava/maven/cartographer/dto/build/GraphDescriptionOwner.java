package org.commonjava.maven.cartographer.dto.build;

import org.commonjava.maven.cartographer.dto.GraphDescription;

public interface GraphDescriptionOwner<T extends GraphDescriptionOwner<T>>
{

    T withGraph( GraphDescription graph );

}