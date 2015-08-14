package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.MultiRenderRequest;

import java.util.Map;

public class MultiRenderRequestBuilder<T extends MultiRenderRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends MultiRenderRequest>
    extends MultiGraphRequestBuilder<T, O, R>
{

    protected GraphComposition graphs;

    private Map<String, String> params;

    public static final class StandaloneMR
        extends MultiRenderRequestBuilder<StandaloneMR, StandaloneRequestOwner<MultiRenderRequest>, MultiRenderRequest>
    {
        public StandaloneMR()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandaloneMR newMultiRenderRecipeBuilder()
    {
        return new StandaloneMR();
    }

    public MultiRenderRequestBuilder( final O owner )
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
        final MultiRenderRequest recipe = new MultiRenderRequest();
        configure( recipe );
        configureMultiGraphs( recipe );
        configureRender( recipe );

        return (R) recipe;
    }

    protected void configureRender( final MultiRenderRequest recipe )
    {
        recipe.setRenderParams( params );
    }

}
