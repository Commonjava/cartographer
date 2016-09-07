/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.inject;

import org.commonjava.cartographer.conf.CartographerConfig;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProvider;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProviderFactory;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.propulsor.deploy.undertow.ui.UIConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class CartoProvider
{

    private PartyLineCacheProviderFactory cacheProviderFactory;

    @Inject
    private CartographerConfig config;

    protected CartoProvider()
    {
    }

    @Produces
    @Default
    @ApplicationScoped
    public synchronized PartyLineCacheProviderFactory getCacheProviderFactory()
    {
        // we do this here to give the configuration time to be populated by the configurator.
        if ( cacheProviderFactory == null )
        {
            cacheProviderFactory = new PartyLineCacheProviderFactory( config.getCacheBasedir() );
        }

        return cacheProviderFactory;
    }
}
