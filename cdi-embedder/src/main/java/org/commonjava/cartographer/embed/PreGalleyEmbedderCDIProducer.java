package org.commonjava.cartographer.embed;

import org.commonjava.cartographer.conf.CartographerConfig;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProviderConfig;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Separate from {@link EmbeddableCDIProducer} because including these parts there create a cyclic dependency with
 * {@link org.commonjava.maven.galley.embed.EmbeddableCDIProducer} in galley.
 */
@ApplicationScoped
public class PreGalleyEmbedderCDIProducer
{
    @Inject
    private CartographerConfig config;

    private PartyLineCacheProviderConfig cacheConfig;

    @PostConstruct
    public void init()
    {
        cacheConfig = new PartyLineCacheProviderConfig( config.getCacheBasedir() );
    }

    @Produces
    @Default
    public PartyLineCacheProviderConfig getConfig()
    {
        return cacheConfig;
    }
}
