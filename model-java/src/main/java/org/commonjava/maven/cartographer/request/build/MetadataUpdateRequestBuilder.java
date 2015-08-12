package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphDescription;
import org.commonjava.maven.cartographer.request.MetadataUpdateRequest;

public class MetadataUpdateRequestBuilder<T extends MetadataUpdateRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends MetadataUpdateRequest>
    extends ProjectGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneMeta
        extends ProjectGraphRequestBuilder<StandaloneMeta, StandaloneRequestOwner<MetadataUpdateRequest>, MetadataUpdateRequest>
    {
        public StandaloneMeta()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandaloneMeta newMetadataRecipeBuilder()
    {
        return new StandaloneMeta();
    }

    private GraphDescription graph;

    public MetadataUpdateRequestBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MetadataUpdateRequest recipe = new MetadataUpdateRequest();
        recipe.setGraph( graph );
        configure( recipe );

        return (R) recipe;
    }

    @Override
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
