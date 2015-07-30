package org.commonjava.maven.cartographer.dto.build;

import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.SingleGraphResolverRecipe;

public class SingleGraphResolverRecipeBuilder<O extends ResolverRecipeOwner<O, SingleGraphResolverRecipe>>
    extends AbstractResolverRecipeBuilder<O, SingleGraphResolverRecipeBuilder<O>, SingleGraphResolverRecipe>
    implements GraphDescriptionOwner<SingleGraphResolverRecipeBuilder<O>>
{

    private GraphDescription graph;

    public static SingleGraphResolverRecipeBuilder<StandaloneRecipeOwner<SingleGraphResolverRecipe>> newSingleGraphResolverRecipeBuilder()
    {
        return new SingleGraphResolverRecipeBuilder<>( new StandaloneRecipeOwner<SingleGraphResolverRecipe>() );
    }

    public SingleGraphResolverRecipeBuilder( final O owner )
    {
        super( owner );
    }

    @Override
    public SingleGraphResolverRecipe build()
    {
        final SingleGraphResolverRecipe recipe = new SingleGraphResolverRecipe();
        recipe.setGraph( graph );
        configure( recipe );

        return recipe;
    }

    public GraphDescriptionBuilder<SingleGraphResolverRecipeBuilder<O>> withNewGraph()
    {
        return new GraphDescriptionBuilder<>( this );
    }

    @Override
    public SingleGraphResolverRecipeBuilder<O> withGraph( final GraphDescription graph )
    {
        this.graph = graph;
        return this;
    }

}
