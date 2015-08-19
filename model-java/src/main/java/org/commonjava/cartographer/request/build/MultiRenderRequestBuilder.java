package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.MultiRenderRequest;

import java.util.Map;

public class MultiRenderRequestBuilder<T extends MultiRenderRequestBuilder<T, R>, R extends MultiRenderRequest>
    extends MultiGraphRequestBuilder<T, R>
{

    protected GraphComposition graphs;

    private Map<String, String> params;

    public static final class StandaloneMR
        extends MultiRenderRequestBuilder<StandaloneMR, MultiRenderRequest>
    {
    }

    public static StandaloneMR newMultiRenderRecipeBuilder()
    {
        return new StandaloneMR();
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
        final R recipe = (R) new MultiRenderRequest();
        configure( recipe );

        return recipe;
    }

    protected void configure( final R recipe )
    {
        recipe.setRenderParams( params );
        super.configure( recipe );
    }

}
