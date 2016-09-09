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
package org.commonjava.maven.cartographer.ftest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.cartographer.INTERNAL.graph.discover.SourceManagerImpl;
import org.commonjava.cartographer.boot.Options;
import org.commonjava.cartographer.client.ClientCartographer;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.boot.BootStatus;
import org.commonjava.propulsor.boot.Booter;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.util.UrlUtils;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import static org.commonjava.maven.cartographer.ftest.testutil.TestFileUtils.readResource;
import static org.commonjava.maven.cartographer.ftest.testutil.TestFileUtils.writeConfigFile;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.jboss.weld.environment.se.WeldContainer;

public class LocalTestDriver
        implements CartoTCKDriver
{

    public static final String MAIN_CONF = "main.conf";

    public static final String TEST_MAIN_CONF = "test-main.conf";

    private Options options;

    private BootStatus bootStatus;

    private Booter booter;

    private PasswordManager passwordManager;

    private HttpFactory httpFactory;

    private SiteConfig siteConfig;

    private TemporaryFolder temp = new TemporaryFolder();

    private Cartographer carto;

    private SourceManagerImpl sourceManager;

    private Consumer<File> configurator = defaultConfigWriter();

    public Consumer<File> defaultConfigWriter()
    {
        return (configDir)->writeConfigFile( configDir, MAIN_CONF, readResource( TEST_MAIN_CONF ) );
    }

    public void setConfigurator( Consumer<File> configurator )
    {
        this.configurator = configurator;
    }

    @Override
    public Cartographer start( TemporaryFolder temp )
            throws Exception
    {
        sourceManager = new SourceManagerImpl();

        temp.create();

        File homeDir = temp.newFolder( "carto-home" );
        File configDir = new File( homeDir, "etc" );
        configDir.mkdirs();

        configurator.accept( configDir );

        final ServiceLoader<BootOptions> loader = ServiceLoader.load( BootOptions.class );
        final BootOptions opts = loader.iterator()
                                          .next();

        options = (Options) opts;
        options.setPort( -1 );
        options.setHomeDir( homeDir.getAbsolutePath() );

        // Should not need this; the configurator should be smart enough to try ${carto.home}/etc/main.conf on its own.
//        options.setConfig( new File( configDir, MAIN_CONF ).getAbsolutePath() );

        booter = new Booter();
        bootStatus = booter.start( options );

        if ( bootStatus == null )
        {
            fail( "No boot status" );
        }

        Throwable t = bootStatus.getError();
        if ( t != null )
        {
            throw new RuntimeException( "Failed to start Cartographer test server.", t );
        }

        assertThat( bootStatus.isSuccess(), equalTo( true ) );

        WeldContainer container = booter.getContainer();
        sourceManager = container.instance().select( SourceManagerImpl.class ).get();

        passwordManager = new MemoryPasswordManager();
        siteConfig = new SiteConfigBuilder( "local-test", formatUrl().toString() ).withRequestTimeoutSeconds( 30 ).build();
        httpFactory = new HttpFactory( passwordManager );

        carto = new ClientCartographer( siteConfig, httpFactory );
        return carto;
    }

    @Override
    public void stop()
            throws Exception
    {
        if ( booter != null && bootStatus != null && bootStatus.isSuccess() )
        {
            booter.stop();
        }

        temp.delete();

        if ( carto != null )
        {
            carto.close();
        }
    }

    @Override
    public void createRepoAlias( String alias, String repoResource )
            throws Exception
    {
        System.out.println( "Aliasing 'test' to: " + repoResource );
        sourceManager.withAlias( "test", repoResource );
    }

    public HttpFactory getHttpFactory()
            throws Exception
    {
        return httpFactory;
    }

    public SiteConfig getSiteConfig()
            throws Exception
    {
        return siteConfig;
    }

    public PasswordManager getPasswordManager()
            throws Exception
    {
        return passwordManager;
    }

    private String formatUrl( String... pathParts )
            throws MalformedURLException
    {
        checkStarted();
        return UrlUtils.buildUrl( String.format( "http://localhost:%d/api", getPort() ), pathParts );
    }

    private void checkStarted()
    {
        if ( booter == null || bootStatus == null || !bootStatus.isSuccess() )
        {
            throw new RuntimeException( "Cannot execute; Cartographer test server is not running." );
        }
    }

    private int getPort()
    {
        checkStarted();
        return options.getPort();
    }
}
