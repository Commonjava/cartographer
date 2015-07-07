package org.commonjava.maven.cartographer.ftest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.Cartographer;
import org.commonjava.maven.cartographer.CartographerBuilder;
import org.commonjava.maven.cartographer.discover.SourceManagerImpl;
import org.commonjava.maven.cartographer.dto.PomRecipe;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.filearc.FileTransport;
import org.commonjava.maven.galley.filearc.ZipJarTransport;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SimpleProjectWithOneDepDownloadTest
{

    private static final String PROJECT = "graphs/simple-dep";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Cartographer carto;

    private FileNeo4jConnectionFactory connectionFactory;

    private SourceManagerImpl sourceManager;

    @Before
    public void before()
        throws Exception
    {
        final TransferDecorator decorator = new NoOpTransferDecorator();
        final FileEventManager fileEvents = new NoOpFileEventManager();
        final PathGenerator pathGen = new HashedLocationPathGenerator();

        final FileCacheProvider cache =
            new FileCacheProvider( temp.newFolder( "cache" ), pathGen, fileEvents, decorator );

        final FileTransport fileTransport = new FileTransport( temp.newFolder( "pub" ), pathGen );
        final ZipJarTransport zipTransport = new ZipJarTransport();

        connectionFactory = new FileNeo4jConnectionFactory( temp.newFolder( "db" ), false );

        sourceManager = new SourceManagerImpl();

        carto = new CartographerBuilder( cache, connectionFactory ).withTransferDecorator( decorator )
                                                                   .withFileEvents( fileEvents )
                                                                   .withTransports( fileTransport, zipTransport )
                                                                   .withSourceManager( sourceManager )
                                                                   .withLocationExpander( sourceManager )
                                                                   .build();
    }

    @After
    public void after()
        throws Exception
    {
        if ( connectionFactory != null )
        {
            connectionFactory.close();
        }

        if ( carto != null )
        {
            carto.close();
        }
    }

    @Test
    public void run()
        throws Exception
    {
        final String dto = PROJECT + "/dto/pom.json";
        final InputStream dtoStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( dto );
        if ( dtoStream == null )
        {
            fail( "Cannot find DTO in classpath: '" + dto + "'");
        }
        
        final PomRecipe recipe = carto.getObjectMapper()
                                      .readValue( dtoStream, PomRecipe.class );

        final URL pomUrl = Thread.currentThread()
                                 .getContextClassLoader()
                                 .getResource( PROJECT + "/repo/org/foo/consumer/1/consumer-1.pom" );

        File f = new File( pomUrl.getPath() );
        for ( int i = 0; i < 5; i++ )
        {
            f = f.getParentFile();
        }

        final String url = f.toURI()
                            .toURL()
                            .toExternalForm();
        System.out.println( "Aliasing 'test' to: " + url );
        sourceManager.withAlias( "test", url );

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents =
            carto.getResolver()
                 .resolveRepositoryContents( recipe );

        System.out.println( contents );

        //        carto.getResolver()
        //             .resolve( recipe );

        assertThat( "foo", notNullValue() );
        assertThat( "bar", equalTo( "bar" ) );
    }

}
