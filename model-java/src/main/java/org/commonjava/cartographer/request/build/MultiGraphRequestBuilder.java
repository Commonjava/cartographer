package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.MultiGraphRequest;

public class MultiGraphRequestBuilder<T extends MultiGraphRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends MultiGraphRequest>
    extends AbstractGraphRequestBuilder<O, T, R>
    implements GraphCompositionOwner<T>
{

    protected GraphComposition graphs;

    public static final class StandaloneMulti
        extends MultiGraphRequestBuilder<StandaloneMulti, StandaloneRequestOwner<MultiGraphRequest>, MultiGraphRequest>
    {
        public StandaloneMulti()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandaloneMulti newMultiGraphResolverRequestBuilder()
    {
        return new StandaloneMulti();
    }

    public MultiGraphRequestBuilder( final O owner )
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
        final MultiGraphRequest recipe = new MultiGraphRequest();
        configure( recipe );
        configureMultiGraphs( recipe );

        return (R) recipe;
    }

    protected void configureMultiGraphs( final MultiGraphRequest recipe )
    {
        recipe.setGraphComposition( graphs );
    }

}
