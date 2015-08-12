package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.MetadataCollationRequest;
import org.commonjava.maven.cartographer.request.MetadataExtractionRequest;

import java.util.Set;

public class MetadataCollationRequestBuilder<T extends MetadataCollationRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends MetadataCollationRequest>
    extends ProjectGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneMeta
        extends MetadataCollationRequestBuilder<StandaloneMeta, StandaloneRequestOwner<MetadataCollationRequest>, MetadataCollationRequest>
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

    private Set<String> keys;

    public MetadataCollationRequestBuilder( final O owner )
    {
        super( owner );
    }

    public T withKeys(Set<String> keys)
    {
        this.keys = keys;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MetadataCollationRequest recipe = new MetadataCollationRequest();
        configureGraph( recipe );
        configureKeys( recipe );
        configure( recipe );

        return (R) recipe;
    }

    protected void configureKeys( MetadataCollationRequest recipe )
    {

    }
}
