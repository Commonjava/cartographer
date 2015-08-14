package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphComposition;

public interface GraphCompositionOwner<T extends GraphCompositionOwner<T>>
{

    T withGraphs( GraphComposition graphs );

}
