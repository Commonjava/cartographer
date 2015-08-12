package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphDescription;
import org.commonjava.maven.cartographer.request.MetadataExtractionRequest;

public class MetadataExtractionRequestBuilder<T extends MetadataExtractionRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends MetadataExtractionRequest>
    extends ProjectGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneMeta
        extends ProjectGraphRequestBuilder<StandaloneMeta, StandaloneRequestOwner<MetadataExtractionRequest>, MetadataExtractionRequest>
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

    public MetadataExtractionRequestBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MetadataExtractionRequest recipe = new MetadataExtractionRequest();
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
