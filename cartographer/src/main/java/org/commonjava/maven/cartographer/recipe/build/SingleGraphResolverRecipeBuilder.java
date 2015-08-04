package org.commonjava.maven.cartographer.recipe.build;

import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.recipe.SingleGraphResolverRecipe;

public class SingleGraphResolverRecipeBuilder<T extends SingleGraphResolverRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends SingleGraphResolverRecipe>
    extends AbstractResolverRecipeBuilder<O, T, R>
    implements GraphDescriptionOwner<T>
{

    private GraphDescription graph;

    public static final class StandaloneSingle
        extends
        SingleGraphResolverRecipeBuilder<StandaloneSingle, StandaloneRecipeOwner<SingleGraphResolverRecipe>, SingleGraphResolverRecipe>
    {
        public StandaloneSingle()
        {
            super( new StandaloneRecipeOwner<>() );
        }
    }

    public static StandaloneSingle newSingleGraphResolverRecipeBuilder()
    {
        return new StandaloneSingle();
    }

    public SingleGraphResolverRecipeBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final SingleGraphResolverRecipe recipe = new SingleGraphResolverRecipe();
        recipe.setGraph( graph );
        configure( recipe );

        return (R) recipe;
    }

    public GraphDescriptionBuilder<T> withNewGraph()
    {
        return new GraphDescriptionBuilder<>( self );
    }

    @Override
    public T withGraph( final GraphDescription graph )
    {
        this.graph = graph;
        return self;
    }

}
