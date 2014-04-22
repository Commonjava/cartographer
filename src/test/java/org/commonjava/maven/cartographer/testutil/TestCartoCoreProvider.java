/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.testutil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jWorkspaceFactory;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProviderConfig;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.nfc.NoOpNotFoundCache;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.testing.core.cdi.TestData;
import org.commonjava.maven.galley.transport.NoOpLocationExpander;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TestCartoCoreProvider
{

    private GraphWorkspaceFactory wsFactory;

    private EGraphManager graphs;

    private final File dbDir;

    private final File cacheDir;

    private NoOpFileEventManager fileEvents;

    private NoOpTransferDecorator transferDecorator;

    private NoOpLocationExpander locationExpander;

    private NoOpNotFoundCache nfc;

    private PasswordManager passwords;

    private Http http;

    private final Set<File> toDelete = new HashSet<File>();

    private FileCacheProviderConfig cacheProviderConfig;

    public TestCartoCoreProvider()
        throws IOException
    {
        dbDir = newTempFile( "database" );
        cacheDir = newTempFile( "cache" );
    }

    public TestCartoCoreProvider( final TemporaryFolder temp )
        throws IOException
    {
        dbDir = temp.newFolder( "database" );
        cacheDir = temp.newFolder( "cache" );

        setup();
    }

    @PostConstruct
    public void setup()
        throws IOException
    {
        cacheProviderConfig = new FileCacheProviderConfig( cacheDir );

        fileEvents = new NoOpFileEventManager();
        transferDecorator = new NoOpTransferDecorator();
        locationExpander = new NoOpLocationExpander();
        nfc = new NoOpNotFoundCache();

        wsFactory = new FileNeo4jWorkspaceFactory( dbDir, false );
        graphs = new EGraphManager( wsFactory );
        passwords = new MemoryPasswordManager();
        http = new HttpImpl( passwords );
    }

    private File newTempFile( final String name )
        throws IOException
    {
        final File dir = File.createTempFile( name, ".dir" );
        FileUtils.forceDelete( dir );
        dir.mkdirs();

        toDelete.add( dir );
        return dir;
    }

    @Produces
    @Default
    @TestData
    public NotFoundCache getNotFoundCache()
    {
        return nfc;
    }

    @Produces
    @Default
    @TestData
    public FileCacheProviderConfig getCacheProviderConfig()
    {
        return cacheProviderConfig;
    }

    @Produces
    @Default
    @TestData
    public PasswordManager getPasswordManager()
    {
        return passwords;
    }

    @Produces
    @Default
    @TestData
    public LocationExpander getLocationExpander()
    {
        return locationExpander;
    }

    @Produces
    @Default
    @TestData
    public Http getHttp()
    {
        return http;
    }

    @Produces
    @Default
    @TestData
    public FileEventManager getFileEventManager()
    {
        return fileEvents;
    }

    @Produces
    @Default
    @TestData
    public TransferDecorator getTransferDecorator()
    {
        return transferDecorator;
    }

    @Produces
    @Default
    @TestData
    public EGraphManager getGraphs()
        throws IOException
    {
        return graphs;
    }

    @PreDestroy
    public void shutdown()
        throws IOException
    {
        graphs.close();
        for ( final File dir : toDelete )
        {
            if ( dir.exists() )
            {
                try
                {
                    FileUtils.forceDelete( dir );
                }
                catch ( final IOException e )
                {
                    LoggerFactory.getLogger( getClass() )
                                 .error( e.getMessage(), e );
                }
            }
        }
    }

    @Produces
    @Default
    @TestData
    public GraphWorkspaceFactory getFactory()
        throws IOException
    {
        return wsFactory;
    }

}
