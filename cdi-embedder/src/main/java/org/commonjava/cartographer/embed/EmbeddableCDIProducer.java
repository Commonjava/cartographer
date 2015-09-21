package org.commonjava.cartographer.embed;

import org.apache.commons.io.IOUtils;
import org.commonjava.cartographer.INTERNAL.graph.discover.SourceManagerImpl;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

/**
 * Created by jdcasey on 9/14/15.
 */
@ApplicationScoped
public class EmbeddableCDIProducer
{

    @Inject
    @Named( "graph-db.dir" )
    private File graphDbDir;

    private RelationshipGraphFactory graphFactory;

    private DiscoverySourceManager sourceManager;

    @PostConstruct
    public void postConstruct()
    {
        graphFactory = new RelationshipGraphFactory( new FileNeo4jConnectionFactory( graphDbDir, false ));
        sourceManager = new SourceManagerImpl();
    }

    @PreDestroy
    public void preDestroy()
    {
        if ( graphFactory != null )
        {
            IOUtils.closeQuietly( graphFactory );
        }
    }

    @Default
    @Produces
    public RelationshipGraphFactory getGraphFactory()
    {
        return graphFactory;
    }

    @Default
    @Produces
    public DiscoverySourceManager getSourceManager()
    {
        return sourceManager;
    }

}
