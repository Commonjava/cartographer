package org.commonjava.maven.cartographer.recipe.build;

import org.commonjava.maven.cartographer.dto.GraphComposition;

public interface GraphCompositionOwner<T extends GraphCompositionOwner<T>>
{

    T withGraphs( GraphComposition graphs );

}
