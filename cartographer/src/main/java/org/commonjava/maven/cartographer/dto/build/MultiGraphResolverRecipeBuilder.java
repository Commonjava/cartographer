package org.commonjava.maven.cartographer.dto.build;

import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.MultiGraphResolverRecipe;

public class MultiGraphResolverRecipeBuilder<T extends MultiGraphResolverRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends MultiGraphResolverRecipe>
    extends AbstractResolverRecipeBuilder<O, T, R>
    implements GraphCompositionOwner<T>
{

    protected GraphComposition graphs;

    public static final class StandaloneMulti
        extends
        MultiGraphResolverRecipeBuilder<StandaloneMulti, StandaloneRecipeOwner<MultiGraphResolverRecipe>, MultiGraphResolverRecipe>
    {
        public StandaloneMulti()
        {
            super( new StandaloneRecipeOwner<>() );
        }
    }

    public static StandaloneMulti newMultiGraphResolverRecipeBuilder()
    {
        return new StandaloneMulti();
    }

    public MultiGraphResolverRecipeBuilder( final O owner )
    {
        super( owner );
    }

    public GraphCompositionBuilder<T> withNewGraphComposition()
    {
        return new GraphCompositionBuilder<>( self );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public T withGraphs( final GraphComposition graphs )
    {
        this.graphs = graphs;
        return (T) this;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MultiGraphResolverRecipe recipe = new MultiGraphResolverRecipe();
        configure( recipe );
        configureMultiGraphs( recipe );

        return (R) recipe;
    }

    protected void configureMultiGraphs( final MultiGraphResolverRecipe recipe )
    {
        recipe.setGraphComposition( graphs );
    }

}
