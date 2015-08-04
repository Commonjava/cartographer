package org.commonjava.maven.cartographer.recipe.build;

import org.commonjava.maven.cartographer.recipe.AbstractResolverRecipe;

public interface ResolverRecipeOwner<T extends ResolverRecipeOwner<T, R>, R extends AbstractResolverRecipe>
{

    T withResolverRecipe( R recipe );

}
