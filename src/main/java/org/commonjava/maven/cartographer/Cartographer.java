package org.commonjava.maven.cartographer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.discover.DiscovererImpl;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.discover.SourceManagerImpl;
import org.commonjava.maven.cartographer.event.CartoEventManager;
import org.commonjava.maven.cartographer.event.NoOpCartoEventManager;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.ops.WorkspaceOps;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.ArtifactManagerImpl;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.TransferManagerImpl;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.filearc.FileTransport;
import org.commonjava.maven.galley.filearc.ZipJarTransport;
import org.commonjava.maven.galley.internal.xfer.DownloadHandler;
import org.commonjava.maven.galley.internal.xfer.ExistenceHandler;
import org.commonjava.maven.galley.internal.xfer.ListingHandler;
import org.commonjava.maven.galley.internal.xfer.UploadHandler;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.NoOpLocationExpander;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;
import org.commonjava.maven.galley.type.StandardTypeMapper;

@ApplicationScoped
public class Cartographer
{

    @Inject
    private CalculationOps calculator;

    @Inject
    private GraphOps grapher;

    @Inject
    private GraphRenderingOps renderer;

    @Inject
    private MetadataOps metadata;

    @Inject
    private ResolveOps resolver;

    @Inject
    private WorkspaceOps workspaces;

    protected Cartographer()
    {
    }

    public Cartographer( final CalculationOps calculator, final GraphOps grapher, final GraphRenderingOps renderer, final MetadataOps metadata,
                         final ResolveOps resolver, final WorkspaceOps workspace )
    {
        this.calculator = calculator;
        this.grapher = grapher;
        this.renderer = renderer;
        this.metadata = metadata;
        this.resolver = resolver;
        this.workspaces = workspace;
    }

    public Cartographer( final String workspaceId, final File resolverCacheDir, final int resolverThreads, final GraphWorkspaceFactory wsFactory )
        throws CartoDataException
    {
        final EGraphManager graphs = new EGraphManager( wsFactory );

        // TODO: This needs to be replaced with a real implementation.
        final CartoEventManager events = new NoOpCartoEventManager();

        final DiscoverySourceManager sourceFactory = new SourceManagerImpl();

        final GraphWorkspaceHolder wsHolder = new GraphWorkspaceHolder();

        final CartoDataManager data = new DefaultCartoDataManager( graphs, wsHolder, events );
        this.workspaces = new WorkspaceOps( data, sourceFactory );

        this.calculator = new CalculationOps( data );
        this.grapher = new GraphOps( data );
        this.renderer = new GraphRenderingOps( data );
        this.metadata = new MetadataOps( data );

        final MavenModelProcessor mmp = new MavenModelProcessor( data );

        // TODO: which password manager makes sense here??
        final Http http = new HttpImpl( new MemoryPasswordManager() );
        final GlobalHttpConfiguration globalConfig = new GlobalHttpConfiguration();

        final TransportManager transport =
            new TransportManagerImpl( new HttpClientTransport( http, globalConfig ), new FileTransport(), new ZipJarTransport() );

        final CacheProvider cache = new FileCacheProvider( resolverCacheDir, new HashedLocationPathGenerator() );

        // TODO: This needs a real implementation, to make the system respond to resolver events.
        final FileEventManager fileEvents = new NoOpFileEventManager();

        // TODO: Probably need something here to verify checksums AT A MINIMUM
        final TransferDecorator decorator = new NoOpTransferDecorator();

        final NotFoundCache nfc = new MemoryNotFoundCache();

        final ExecutorService aggExecutor = Executors.newFixedThreadPool( resolverThreads < 2 ? 2 : resolverThreads );
        final ExecutorService transportExecutor = Executors.newFixedThreadPool( resolverThreads < 2 ? 2 : resolverThreads );
        final ExecutorService batchExecutor = Executors.newFixedThreadPool( resolverThreads < 2 ? 2 : resolverThreads );

        final DownloadHandler dh = new DownloadHandler( nfc, transportExecutor );
        final UploadHandler uh = new UploadHandler( nfc, transportExecutor );
        final ListingHandler lh = new ListingHandler( nfc );
        final ExistenceHandler eh = new ExistenceHandler( nfc );

        final TransferManager xferMgr = new TransferManagerImpl( transport, cache, nfc, fileEvents, decorator, dh, uh, lh, eh, batchExecutor );

        final ProjectRelationshipDiscoverer discoverer = new DiscovererImpl( data, mmp, xferMgr );
        final GraphAggregator aggregator = new DefaultGraphAggregator( data, discoverer, aggExecutor );

        final ArtifactManager artifacts = new ArtifactManagerImpl( xferMgr, new NoOpLocationExpander(), new StandardTypeMapper() );
        this.resolver = new ResolveOps( data, sourceFactory, discoverer, aggregator, artifacts );
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
