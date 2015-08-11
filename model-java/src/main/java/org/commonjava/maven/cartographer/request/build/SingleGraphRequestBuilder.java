package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphDescription;
import org.commonjava.maven.cartographer.request.SingleGraphRequest;

public class SingleGraphRequestBuilder<T extends SingleGraphRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends SingleGraphRequest>
    extends AbstractGraphRequestBuilder<O, T, R>
    implements GraphDescriptionOwner<T>
{

    private GraphDescription graph;

    public static final class StandaloneSingle
        extends SingleGraphRequestBuilder<StandaloneSingle, StandaloneRequestOwner<SingleGraphRequest>, SingleGraphRequest>
    {
        public StandaloneSingle()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandaloneSingle newSingleGraphResolverRecipeBuilder()
    {
        return new StandaloneSingle();
    }

    public SingleGraphRequestBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final SingleGraphRequest recipe = new SingleGraphRequest();
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
