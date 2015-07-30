package org.commonjava.maven.cartographer.dto.build;

import org.commonjava.maven.cartographer.dto.AbstractResolverRecipe;

public interface ResolverRecipeOwner<T extends ResolverRecipeOwner<T, R>, R extends AbstractResolverRecipe>
{

    T withResolverRecipe( R recipe );

}
