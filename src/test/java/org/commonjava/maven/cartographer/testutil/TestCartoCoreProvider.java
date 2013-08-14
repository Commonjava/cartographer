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
import org.commonjava.maven.atlas.graph.spi.EGraphDriver;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4JEGraphDriver;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProviderConfig;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.util.logging.Logger;
import org.junit.rules.TemporaryFolder;

@ApplicationScoped
public class TestCartoCoreProvider
{

    private EGraphDriver driver;

    private EGraphManager graphs;

    private final File dbDir;

    private final File cacheDir;

    private NoOpFileEventManager fileEvents;

    private NoOpTransferDecorator transferDecorator;

    private PasswordManager passwords;

    private Http http;

    private final Set<File> toDelete = new HashSet<>();

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

        driver = new FileNeo4JEGraphDriver( dbDir );
        graphs = new EGraphManager( driver );
        fileEvents = new NoOpFileEventManager();
        transferDecorator = new NoOpTransferDecorator();

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
                    new Logger( getClass() ).error( e.getMessage(), e );
                }
            }
        }
    }

    @Produces
    @Default
    @TestData
    public EGraphDriver getDriver()
        throws IOException
    {
        return driver;
    }

}
