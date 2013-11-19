package org.commonjava.maven.cartographer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScanner;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
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
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.ArtifactMetadataManager;
import org.commonjava.maven.galley.maven.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.defaults.StandardMavenPluginImplications;
import org.commonjava.maven.galley.maven.internal.ArtifactManagerImpl;
import org.commonjava.maven.galley.maven.internal.ArtifactMetadataManagerImpl;
import org.commonjava.maven.galley.maven.internal.version.VersionResolverImpl;
import org.commonjava.maven.galley.maven.model.view.XPathManager;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.maven.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.type.TypeMapper;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.spi.transport.Transport;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.NoOpLocationExpander;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;

public class CartographerBuilder
{
    private GraphWorkspaceFactory wsFactory;

    private int resolverThreads;

    private File resolverCacheDir;

    private DiscoverySourceManager sourceManager;

    private TransportManager transportManager;

    private TransferDecorator transferDecorator;

    private TransferManager transferMgr;

    private ArtifactManager artifactManager;

    private LocationExpander locationExpander;

    private TypeMapper typeMapper;

    private MavenPluginImplications pluginImplications;

    private MavenPluginDefaults pluginDefaults;

    private Collection<MetadataScanner> metadataScanners;

    private ProjectRelationshipDiscoverer discoverer;

    private Set<Transport> transports;

    private PasswordManager passwordManager;

    private VersionResolver versionResolver;

    private MavenMetadataReader metadataReader;

    private ArtifactMetadataManager artifactMetadataManager;

    public CartographerBuilder( final String workspaceId, final File resolverCacheDir, final int resolverThreads,
                                final GraphWorkspaceFactory wsFactory )
        throws CartoDataException
    {
        this.resolverCacheDir = resolverCacheDir;
        this.resolverThreads = resolverThreads;
        this.wsFactory = wsFactory;
    }

    public Cartographer build()
        throws CartoDataException
    {
        final EGraphManager graphs = new EGraphManager( wsFactory );

        // TODO: This needs to be replaced with a real implementation.
        final CartoEventManager events = new NoOpCartoEventManager();

        if ( this.sourceManager == null )
        {
            this.sourceManager = new SourceManagerImpl();
        }

        final GraphWorkspaceHolder wsHolder = new GraphWorkspaceHolder();

        if ( passwordManager == null )
        {
            passwordManager = new MemoryPasswordManager();
        }

        // TODO: which password manager makes sense here??
        final Http http = new HttpImpl( passwordManager );
        final GlobalHttpConfiguration globalConfig = new GlobalHttpConfiguration();

        if ( this.transports == null )
        {
            this.transports = new HashSet<>();
        }

        if ( this.transports.isEmpty() )
        {
            transports.add( new HttpClientTransport( http, globalConfig ) );
            transports.add( new FileTransport() );
            transports.add( new ZipJarTransport() );
        }

        if ( this.transportManager == null )
        {
            this.transportManager = new TransportManagerImpl( transports.toArray( new Transport[transports.size()] ) );
        }

        // TODO: This needs a real implementation, to make the system respond to resolver events.
        final FileEventManager fileEvents = new NoOpFileEventManager();

        // TODO: Probably need something here to verify checksums AT A MINIMUM
        if ( this.transferDecorator == null )
        {
            this.transferDecorator = new NoOpTransferDecorator();
        }

        final CacheProvider cache = new FileCacheProvider( resolverCacheDir, new HashedLocationPathGenerator(), fileEvents, transferDecorator );

        final NotFoundCache nfc = new MemoryNotFoundCache();

        final ExecutorService aggExecutor = Executors.newFixedThreadPool( resolverThreads < 2 ? 2 : resolverThreads );
        final ExecutorService transportExecutor = Executors.newFixedThreadPool( resolverThreads < 2 ? 2 : resolverThreads );
        final ExecutorService batchExecutor = Executors.newFixedThreadPool( resolverThreads < 2 ? 2 : resolverThreads );
        final ExecutorService resolveExecutor = Executors.newFixedThreadPool( 10 );

        final DownloadHandler dh = new DownloadHandler( nfc, transportExecutor );
        final UploadHandler uh = new UploadHandler( nfc, transportExecutor );
        final ListingHandler lh = new ListingHandler( nfc );
        final ExistenceHandler eh = new ExistenceHandler( nfc );

        if ( this.transferMgr == null )
        {
            this.transferMgr = new TransferManagerImpl( transportManager, cache, nfc, fileEvents, dh, uh, lh, eh, batchExecutor );
        }

        if ( this.locationExpander == null )
        {
            this.locationExpander = new NoOpLocationExpander();
        }
        if ( this.typeMapper == null )
        {
            this.typeMapper = new StandardTypeMapper();
        }

        final XMLInfrastructure xml = new XMLInfrastructure();

        if ( this.pluginImplications == null )
        {
            this.pluginImplications = new StandardMavenPluginImplications( xml );
        }
        if ( this.pluginDefaults == null )
        {
            this.pluginDefaults = new StandardMaven304PluginDefaults();
        }

        if ( this.artifactMetadataManager == null )
        {
            this.artifactMetadataManager = new ArtifactMetadataManagerImpl( transferMgr, locationExpander );
        }

        final XPathManager xpath = new XPathManager();

        if ( this.metadataReader == null )
        {
            this.metadataReader = new MavenMetadataReader( xml, artifactMetadataManager, xpath );
        }

        if ( this.versionResolver == null )
        {
            this.versionResolver = new VersionResolverImpl( metadataReader );
        }

        if ( this.artifactManager == null )
        {
            this.artifactManager = new ArtifactManagerImpl( transferMgr, locationExpander, typeMapper, versionResolver );
        }

        final MavenPomReader pomReader = new MavenPomReader( xml, artifactManager, xpath, pluginDefaults, pluginImplications );

        if ( this.metadataScanners == null )
        {
            this.metadataScanners = new ArrayList<>();
        }

        // TODO: Add some scanners.
        final MetadataScannerSupport scannerSupport = new MetadataScannerSupport( metadataScanners );

        final CartoDataManager data = new DefaultCartoDataManager( graphs, wsHolder, events );

        final MavenModelProcessor mmp = new MavenModelProcessor( data );

        if ( this.discoverer == null )
        {
            this.discoverer = new DiscovererImpl( mmp, pomReader, artifactManager, data, new PatcherSupport(), scannerSupport );
        }

        final GraphAggregator aggregator = new DefaultGraphAggregator( data, discoverer, aggExecutor );

        final WorkspaceOps workspaceOps = new WorkspaceOps( data, sourceManager );
        final CalculationOps calculationOps = new CalculationOps( data );
        final GraphOps graphOps = new GraphOps( data );
        final GraphRenderingOps graphRenderingOps = new GraphRenderingOps( data );
        final ResolveOps resolveOps = new ResolveOps( calculationOps, data, sourceManager, discoverer, aggregator, artifactManager, resolveExecutor );

        final MetadataOps metadataOps = new MetadataOps( data, artifactManager, pomReader, scannerSupport, sourceManager, resolveOps, calculationOps );

        return new Cartographer( data, calculationOps, graphOps, graphRenderingOps, metadataOps, resolveOps, workspaceOps );
    }

    public GraphWorkspaceFactory getWsFactory()
    {
        return wsFactory;
    }

    public int getResolverThreads()
    {
        return resolverThreads;
    }

    public File getResolverCacheDir()
    {
        return resolverCacheDir;
    }

    public DiscoverySourceManager getSourceManager()
    {
        return sourceManager;
    }

    public TransportManager getTransportManager()
    {
        return transportManager;
    }

    public TransferDecorator getTransferDecorator()
    {
        return transferDecorator;
    }

    public TransferManager getTransferMgr()
    {
        return transferMgr;
    }

    public ArtifactManager getArtifactManager()
    {
        return artifactManager;
    }

    public LocationExpander getLocationExpander()
    {
        return locationExpander;
    }

    public TypeMapper getTypeMapper()
    {
        return typeMapper;
    }

    public MavenPluginImplications getPluginImplications()
    {
        return pluginImplications;
    }

    public MavenPluginDefaults getPluginDefaults()
    {
        return pluginDefaults;
    }

    public Collection<MetadataScanner> getMetadataScanners()
    {
        return metadataScanners;
    }

    public ProjectRelationshipDiscoverer getDiscoverer()
    {
        return discoverer;
    }

    public Set<Transport> getTransports()
    {
        return transports;
    }

    public CartographerBuilder withWsFactory( final GraphWorkspaceFactory wsFactory )
    {
        this.wsFactory = wsFactory;
        return this;
    }

    public CartographerBuilder withResolverThreads( final int resolverThreads )
    {
        this.resolverThreads = resolverThreads;
        return this;
    }

    public CartographerBuilder withResolverCacheDir( final File resolverCacheDir )
    {
        this.resolverCacheDir = resolverCacheDir;
        return this;
    }

    public CartographerBuilder withSourceManager( final DiscoverySourceManager sourceManager )
    {
        this.sourceManager = sourceManager;
        return this;
    }

    public CartographerBuilder withTransportManager( final TransportManager transportManager )
    {
        this.transportManager = transportManager;
        return this;
    }

    public CartographerBuilder withTransferDecorator( final TransferDecorator transferDecorator )
    {
        this.transferDecorator = transferDecorator;
        return this;
    }

    public CartographerBuilder withTransferMgr( final TransferManager transferMgr )
    {
        this.transferMgr = transferMgr;
        return this;
    }

    public CartographerBuilder withArtifactManager( final ArtifactManager artifactManager )
    {
        this.artifactManager = artifactManager;
        return this;
    }

    public CartographerBuilder withLocationExpander( final LocationExpander locationExpander )
    {
        this.locationExpander = locationExpander;
        return this;
    }

    public CartographerBuilder withTypeMapper( final TypeMapper typeMapper )
    {
        this.typeMapper = typeMapper;
        return this;
    }

    public CartographerBuilder withPluginImplications( final MavenPluginImplications pluginImplications )
    {
        this.pluginImplications = pluginImplications;
        return this;
    }

    public CartographerBuilder withPluginDefaults( final MavenPluginDefaults pluginDefaults )
    {
        this.pluginDefaults = pluginDefaults;
        return this;
    }

    public CartographerBuilder withMetadataScanners( final Collection<MetadataScanner> metadataScanners )
    {
        this.metadataScanners = metadataScanners;
        return this;
    }

    public CartographerBuilder withDiscoverer( final ProjectRelationshipDiscoverer discoverer )
    {
        this.discoverer = discoverer;
        return this;
    }

    public CartographerBuilder withTransports( final Set<Transport> transports )
    {
        this.transports = transports;
        return this;
    }

    public CartographerBuilder withPasswordManager( final PasswordManager passwordManager )
    {
        this.passwordManager = passwordManager;
        return this;
    }

    public PasswordManager getPasswordManager()
    {
        return passwordManager;
    }

    public void setWsFactory( final GraphWorkspaceFactory wsFactory )
    {
        this.wsFactory = wsFactory;
    }

    public void setResolverThreads( final int resolverThreads )
    {
        this.resolverThreads = resolverThreads;
    }

    public void setResolverCacheDir( final File resolverCacheDir )
    {
        this.resolverCacheDir = resolverCacheDir;
    }

    public void setSourceManager( final DiscoverySourceManager sourceManager )
    {
        this.sourceManager = sourceManager;
    }

    public void setTransportManager( final TransportManager transportManager )
    {
        this.transportManager = transportManager;
    }

    public void setTransferDecorator( final TransferDecorator transferDecorator )
    {
        this.transferDecorator = transferDecorator;
    }

    public void setTransferMgr( final TransferManager transferMgr )
    {
        this.transferMgr = transferMgr;
    }

    public void setArtifactManager( final ArtifactManager artifactManager )
    {
        this.artifactManager = artifactManager;
    }

    public void setLocationExpander( final LocationExpander locationExpander )
    {
        this.locationExpander = locationExpander;
    }

    public void setTypeMapper( final TypeMapper typeMapper )
    {
        this.typeMapper = typeMapper;
    }

    public void setPluginImplications( final MavenPluginImplications pluginImplications )
    {
        this.pluginImplications = pluginImplications;
    }

    public void setPluginDefaults( final MavenPluginDefaults pluginDefaults )
    {
        this.pluginDefaults = pluginDefaults;
    }

    public void setMetadataScanners( final Collection<MetadataScanner> metadataScanners )
    {
        this.metadataScanners = metadataScanners;
    }

    public void setDiscoverer( final ProjectRelationshipDiscoverer discoverer )
    {
        this.discoverer = discoverer;
    }

    public void setTransports( final Set<Transport> transports )
    {
        this.transports = transports;
    }

    public void setPasswordManager( final PasswordManager passwordManager )
    {
        this.passwordManager = passwordManager;
    }
}