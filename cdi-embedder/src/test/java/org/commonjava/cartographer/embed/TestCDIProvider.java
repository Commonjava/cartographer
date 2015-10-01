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

import org.commonjava.maven.galley.cache.FileCacheProviderConfig;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
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

    private FileCacheProviderConfig config;

    private File dbDir;

    @PostConstruct
    public void start()
    {
        try
        {
            temp.create();
            dbDir = temp.newFolder( "db" );
            config = new FileCacheProviderConfig( temp.newFolder() ).withAliasLinking( true );
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
    @Named( "graph-db.dir" )
    public File getGraphDbDir()
    {
        return dbDir;
    }

    @Produces
    @Default
    public FileCacheProviderConfig getConfig()
    {

        return config;
    }
}
