package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphComposition;

public interface GraphCompositionOwner<T extends GraphCompositionOwner<T>>
{

    T withGraphs( GraphComposition graphs );

}
