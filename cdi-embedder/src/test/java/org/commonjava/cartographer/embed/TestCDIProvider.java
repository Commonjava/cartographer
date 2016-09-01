/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.embed;

import org.commonjava.cartographer.conf.CartographerConfig;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProvider;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProviderConfig;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;

/**
 * Created by jdcasey on 9/14/15.
 */
@ApplicationScoped
public class TestCDIProvider
{
    private TemporaryFolder temp = new TemporaryFolder();

    private PartyLineCacheProvider cacheProvider;

    @Inject
    private CartographerConfig cartoConfig;

    @Inject
    private PathGenerator pathGenerator;

    @Inject
    private FileEventManager eventManager;

    @Inject
    private TransferDecorator transferDecorator;

    @PostConstruct
    public void start()
    {
        try
        {
            temp.create();
        }
        catch ( IOException e )
        {
            Assert.fail( "Failed to init temp folder fro file cache." );
        }
    }

    @PreDestroy
    public void stop()
    {
        temp.delete();
    }

    @Produces
    @Default
    public synchronized PartyLineCacheProvider getCacheProvider()
    {
        // we do this here to give the configuration time to be populated by the configurator.
        if ( cacheProvider == null )
        {
            cacheProvider = new PartyLineCacheProvider( cartoConfig.getCacheBasedir(), pathGenerator, eventManager,
                                                        transferDecorator );
        }
        return cacheProvider;
    }
}
