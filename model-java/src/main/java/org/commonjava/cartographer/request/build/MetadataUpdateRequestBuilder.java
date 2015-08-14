package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.MetadataUpdateRequest;

public class MetadataUpdateRequestBuilder<T extends MetadataUpdateRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends MetadataUpdateRequest>
    extends ProjectGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneMeta
        extends MetadataUpdateRequestBuilder<StandaloneMeta, StandaloneRequestOwner<MetadataUpdateRequest>, MetadataUpdateRequest>
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

    public MetadataUpdateRequestBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MetadataUpdateRequest recipe = new MetadataUpdateRequest();
        configureGraph( recipe );
        configure( recipe );

        return (R) recipe;
    }
}
