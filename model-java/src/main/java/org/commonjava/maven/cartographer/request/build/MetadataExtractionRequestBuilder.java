package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphDescription;
import org.commonjava.maven.cartographer.request.MetadataExtractionRequest;

public class MetadataExtractionRequestBuilder<T extends MetadataExtractionRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends MetadataExtractionRequest>
    extends ProjectGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneMeta
        extends MetadataExtractionRequestBuilder<StandaloneMeta, StandaloneRequestOwner<MetadataExtractionRequest>, MetadataExtractionRequest>
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

    public MetadataExtractionRequestBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MetadataExtractionRequest recipe = new MetadataExtractionRequest();
        configureGraph( recipe );
        configure( recipe );

        return (R) recipe;
    }
}
