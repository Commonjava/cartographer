package org.commonjava.maven.cartographer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4JEGraphDriver;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.discover.SimpleDiscoverer;
import org.commonjava.maven.cartographer.discover.SimpleSourceManager;
import org.commonjava.maven.cartographer.event.CartoEventManager;
import org.commonjava.maven.cartographer.event.NoOpCartoEventManager;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.ops.WorkspaceOps;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.TransferManagerImpl;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.CacheProvider;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.FileEventManager;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.filearc.FileTransport;
import org.commonjava.maven.galley.filearc.ZipJarTransport;
import org.commonjava.maven.galley.io.HashPerRepoPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.io.TransferDecorator;
import org.commonjava.maven.galley.transport.SimpleTransportManager;
import org.commonjava.maven.galley.transport.TransportManager;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;
import org.commonjava.maven.galley.transport.htcli.internal.HttpImpl;

public class Cartographer
{

    private final CalculationOps calculator;

    private final GraphOps grapher;

    private final GraphRenderingOps renderer;

    private final MetadataOps metadata;

    private final ResolveOps resolver;

    private final WorkspaceOps workspaces;

    public Cartographer( final CalculationOps calculator, final GraphOps grapher, final GraphRenderingOps renderer,
                         final MetadataOps metadata, final ResolveOps resolver, final WorkspaceOps workspace )
    {
        this.calculator = calculator;
        this.grapher = grapher;
        this.renderer = renderer;
        this.metadata = metadata;
        this.resolver = resolver;
        this.workspaces = workspace;
    }

    public Cartographer( final String workspaceId, final File depgraphDbDir, final File resolverCacheDir,
                         final int resolverThreads )
        throws CartoDataException
    {
        // FIXME: Neo4J driver prevents ASL2.0!!!
        final EGraphManager graphs = new EGraphManager( new FileNeo4JEGraphDriver( depgraphDbDir ) );

        // TODO: This needs to be replaced with a real implementation.
        final CartoEventManager events = new NoOpCartoEventManager();

        final DiscoverySourceManager sourceFactory = new SimpleSourceManager();

        this.workspaces = new WorkspaceOps( graphs, sourceFactory );

        final GraphWorkspace ws = workspaces.get( workspaceId );
        final CartoDataManager data = new DefaultCartoDataManager( graphs, ws, events );

        this.calculator = new CalculationOps( data );
        this.grapher = new GraphOps( data );
        this.renderer = new GraphRenderingOps( data );
        this.metadata = new MetadataOps( data );

        final MavenModelProcessor mmp = new MavenModelProcessor( data );

        // TODO: which password manager makes sense here??
        final Http http = new HttpImpl( new MemoryPasswordManager() );
        final GlobalHttpConfiguration globalConfig = new GlobalHttpConfiguration();

        final TransportManager transport =
            new SimpleTransportManager( new HttpClientTransport( http, globalConfig ), new FileTransport(),
                                        new ZipJarTransport() );

        final CacheProvider cache = new FileCacheProvider( resolverCacheDir, new HashPerRepoPathGenerator() );

        // TODO: This needs a real implementation, to make the system respond to resolver events.
        final FileEventManager fileEvents = new NoOpFileEventManager();

        // TODO: Probably need something here to verify checksums AT A MINIMUM
        final TransferDecorator decorator = new NoOpTransferDecorator();

        final ExecutorService executor = Executors.newFixedThreadPool( resolverThreads < 2 ? 2 : resolverThreads );

        final TransferManager xferMgr = new TransferManagerImpl( transport, cache, fileEvents, decorator, executor );

        final ProjectRelationshipDiscoverer discoverer = new SimpleDiscoverer( data, mmp, xferMgr );
        final GraphAggregator aggregator = new DefaultGraphAggregator( data, discoverer, executor );
        this.resolver = new ResolveOps( data, sourceFactory, discoverer, aggregator );
    }

    public CalculationOps getCalculator()
    {
        return calculator;
    }

    public GraphOps getGrapher()
    {
        return grapher;
    }

    public GraphRenderingOps getRenderer()
    {
        return renderer;
    }

    public MetadataOps getMetadata()
    {
        return metadata;
    }

    public ResolveOps getResolver()
    {
        return resolver;
    }

    public WorkspaceOps getWorkspaces()
    {
        return workspaces;
    }

}
