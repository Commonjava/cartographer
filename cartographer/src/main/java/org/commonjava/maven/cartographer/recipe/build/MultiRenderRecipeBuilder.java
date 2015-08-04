package org.commonjava.maven.cartographer.recipe.build;

import java.util.Map;

import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.recipe.MultiRenderRecipe;

public class MultiRenderRecipeBuilder<T extends MultiRenderRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends MultiRenderRecipe>
    extends MultiGraphResolverRecipeBuilder<T, O, R>
{

    protected GraphComposition graphs;

    private Map<String, String> params;

    public static final class StandaloneMR
        extends MultiRenderRecipeBuilder<StandaloneMR, StandaloneRecipeOwner<MultiRenderRecipe>, MultiRenderRecipe>
    {
        public StandaloneMR()
        {
            super( new StandaloneRecipeOwner<>() );
        }
    }

    public static StandaloneMR newMultiRenderRecipeBuilder()
    {
        return new StandaloneMR();
    }

    public MultiRenderRecipeBuilder( final O owner )
    {
        super( owner );
    }

    public T withRenderParameters( final Map<String, String> params )
    {
        this.params = params;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MultiRenderRecipe recipe = new MultiRenderRecipe();
        configure( recipe );
        configureMultiGraphs( recipe );
        configureRender( recipe );

        return (R) recipe;
    }

    protected void configureRender( final MultiRenderRecipe recipe )
    {
        recipe.setRenderParams( params );
    }

}
