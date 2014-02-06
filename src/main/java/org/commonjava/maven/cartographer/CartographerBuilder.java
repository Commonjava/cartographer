/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.cartographer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.commonjava.cdi.util.weft.NamedThreadFactory;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.discover.DiscovererImpl;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.discover.SourceManagerImpl;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScanner;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.discover.post.patch.DepgraphPatcher;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
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
import org.commonjava.maven.galley.maven.internal.ArtifactManagerImpl;
import org.commonjava.maven.galley.maven.internal.ArtifactMetadataManagerImpl;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMavenPluginImplications;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.internal.version.VersionResolverImpl;
import org.commonjava.maven.galley.maven.model.view.XPathManager;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.spi.transport.Transport;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.NoOpLocationExpander;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
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

    private Set<Transport> transports = new LinkedHashSet<Transport>();

    private PasswordManager passwordManager;

    private VersionResolver versionResolver;

    private MavenMetadataReader metadataReader;

    private ArtifactMetadataManager artifactMetadataManager;

    private HttpImpl http;

    private GlobalHttpConfiguration globalHttpConfig;

    private EGraphManager graphs;

    private NoOpCartoEventManager events;

    private GraphWorkspaceHolder wsHolder;

    private NoOpFileEventManager fileEvents;

    private FileCacheProvider cache;

    private MemoryNotFoundCache nfc;

    private ExecutorService aggregatorExecutor;

    private ExecutorService transportExecutor;

    private ExecutorService batchExecutor;

    private ExecutorService resolveExecutor;

    private DownloadHandler downloadHandler;

    private UploadHandler uploadHandler;

    private ListingHandler listingHandler;

    private ExistenceHandler existenceHandler;

    private XMLInfrastructure xml;

    private XPathManager xpath;

    private MavenPomReader pomReader;

    private MetadataScannerSupport scannerSupport;

    private DefaultCartoDataManager database;

    private Collection<DepgraphPatcher> depgraphPatchers;

    private PatcherSupport patcherSupport;

    public CartographerBuilder( final String workspaceId, final File resolverCacheDir, final int resolverThreads,
                                final GraphWorkspaceFactory wsFactory )
        throws CartoDataException
    {
        this.resolverCacheDir = resolverCacheDir;
        this.resolverThreads = resolverThreads;
        this.wsFactory = wsFactory;
    }

    public CartographerBuilder initHttpComponents()
    {
        if ( passwordManager == null )
        {
            passwordManager = new MemoryPasswordManager();
        }

        // TODO: which password manager makes sense here??
        if ( http == null )
        {
            http = new HttpImpl( passwordManager );
        }
        if ( globalHttpConfig == null )
        {
            globalHttpConfig = new GlobalHttpConfiguration();
        }

        return this;
    }

    public CartographerBuilder withDefaultTransports()
    {
        initHttpComponents();
        transports.add( new HttpClientTransport( http, globalHttpConfig ) );
        transports.add( new FileTransport() );
        transports.add( new ZipJarTransport() );

        return this;
    }

    public CartographerBuilder withTransport( final Transport transport )
    {
        transports.add( transport );
        return this;
    }

    public Cartographer build()
        throws CartoDataException
    {
        if ( graphs == null )
        {
            graphs = new EGraphManager( wsFactory );
        }

        // TODO: This needs to be replaced with a real implementation.
        if ( events == null )
        {
            events = new NoOpCartoEventManager();
        }

        if ( this.sourceManager == null )
        {
            this.sourceManager = new SourceManagerImpl();
        }

        if ( wsHolder == null )
        {
            wsHolder = new GraphWorkspaceHolder();
        }

        initHttpComponents();

        if ( this.transportManager == null )
        {
            this.transportManager = new TransportManagerImpl( transports.toArray( new Transport[transports.size()] ) );
        }

        // TODO: This needs a real implementation, to make the system respond to resolver events.
        if ( fileEvents == null )
        {
            fileEvents = new NoOpFileEventManager();
        }

        // TODO: Probably need something here to verify checksums AT A MINIMUM
        if ( this.transferDecorator == null )
        {
            this.transferDecorator = new NoOpTransferDecorator();
        }

        if ( cache == null )
        {
            cache = new FileCacheProvider( resolverCacheDir, new HashedLocationPathGenerator(), fileEvents, transferDecorator );
        }

        if ( nfc == null )
        {
            nfc = new MemoryNotFoundCache();
        }

        if ( aggregatorExecutor == null )
        {
            aggregatorExecutor =
                Executors.newScheduledThreadPool( resolverThreads < 2 ? 2 : resolverThreads, new NamedThreadFactory( "carto-aggregator", true, 8 ) );
        }

        if ( transportExecutor == null )
        {
            transportExecutor =
                Executors.newScheduledThreadPool( resolverThreads < 2 ? 2 : resolverThreads, new NamedThreadFactory( "galley-transport", true, 8 ) );
        }

        if ( batchExecutor == null )
        {
            batchExecutor =
                Executors.newScheduledThreadPool( resolverThreads < 2 ? 2 : resolverThreads, new NamedThreadFactory( "galley-batch", true, 8 ) );
        }

        if ( resolveExecutor == null )
        {
            resolveExecutor = Executors.newScheduledThreadPool( 10, new NamedThreadFactory( "carto-resolve", true, 8 ) );
        }

        if ( downloadHandler == null )
        {
            downloadHandler = new DownloadHandler( nfc, transportExecutor );
        }
        if ( uploadHandler == null )
        {
            uploadHandler = new UploadHandler( nfc, transportExecutor );
        }
        if ( listingHandler == null )
        {
            listingHandler = new ListingHandler( nfc );
        }
        if ( existenceHandler == null )
        {
            existenceHandler = new ExistenceHandler( nfc );
        }

        if ( this.transferMgr == null )
        {
            this.transferMgr =
                new TransferManagerImpl( transportManager, cache, nfc, fileEvents, downloadHandler, uploadHandler, listingHandler, existenceHandler,
                                         batchExecutor );
        }

        if ( this.locationExpander == null )
        {
            this.locationExpander = new NoOpLocationExpander();
        }
        if ( this.typeMapper == null )
        {
            this.typeMapper = new StandardTypeMapper();
        }

        if ( xml == null )
        {
            xml = new XMLInfrastructure();
        }

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

        if ( xpath == null )
        {
            xpath = new XPathManager();
        }

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

        if ( pomReader == null )
        {
            pomReader = new MavenPomReader( xml, artifactManager, xpath, pluginDefaults, pluginImplications );
        }

        if ( this.metadataScanners == null )
        {
            this.metadataScanners = new ArrayList<MetadataScanner>();
        }

        // TODO: Add some scanners.
        if ( scannerSupport == null )
        {
            scannerSupport = new MetadataScannerSupport( metadataScanners );
        }

        if ( this.depgraphPatchers == null )
        {
            this.depgraphPatchers = new ArrayList<DepgraphPatcher>();
        }

        if ( patcherSupport == null )
        {
            this.patcherSupport = new PatcherSupport( this.depgraphPatchers.toArray( new DepgraphPatcher[this.depgraphPatchers.size()] ) );
        }

        if ( database == null )
        {
            database = new DefaultCartoDataManager( graphs, wsHolder, events );
        }

        final MavenModelProcessor mmp = new MavenModelProcessor( database );

        if ( this.discoverer == null )
        {
            this.discoverer = new DiscovererImpl( mmp, pomReader, artifactManager, database, patcherSupport, scannerSupport );
        }

        final GraphAggregator aggregator = new DefaultGraphAggregator( database, discoverer, aggregatorExecutor );

        final WorkspaceOps workspaceOps = new WorkspaceOps( database, sourceManager );
        final CalculationOps calculationOps = new CalculationOps( database );
        final GraphOps graphOps = new GraphOps( database );
        final GraphRenderingOps graphRenderingOps = new GraphRenderingOps( database );
        final ResolveOps resolveOps =
            new ResolveOps( calculationOps, database, sourceManager, discoverer, aggregator, artifactManager, resolveExecutor );

        final MetadataOps metadataOps =
            new MetadataOps( database, artifactManager, pomReader, scannerSupport, sourceManager, resolveOps, calculationOps );

        return new Cartographer( database, calculationOps, graphOps, graphRenderingOps, metadataOps, resolveOps, workspaceOps );
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

    public CartographerBuilder withDepgraphPatchers( final Collection<DepgraphPatcher> patchers )
    {
        this.depgraphPatchers = patchers;
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

    public VersionResolver getVersionResolver()
    {
        return versionResolver;
    }

    public MavenMetadataReader getMetadataReader()
    {
        return metadataReader;
    }

    public ArtifactMetadataManager getArtifactMetadataManager()
    {
        return artifactMetadataManager;
    }

    public HttpImpl getHttp()
    {
        return http;
    }

    public GlobalHttpConfiguration getGlobalHttpConfig()
    {
        return globalHttpConfig;
    }

    public EGraphManager getGraphs()
    {
        return graphs;
    }

    public NoOpCartoEventManager getEvents()
    {
        return events;
    }

    public GraphWorkspaceHolder getWsHolder()
    {
        return wsHolder;
    }

    public NoOpFileEventManager getFileEvents()
    {
        return fileEvents;
    }

    public FileCacheProvider getCache()
    {
        return cache;
    }

    public MemoryNotFoundCache getNfc()
    {
        return nfc;
    }

    public ExecutorService getAggregatorExecutor()
    {
        return aggregatorExecutor;
    }

    public ExecutorService getTransportExecutor()
    {
        return transportExecutor;
    }

    public ExecutorService getBatchExecutor()
    {
        return batchExecutor;
    }

    public ExecutorService getResolveExecutor()
    {
        return resolveExecutor;
    }

    public DownloadHandler getDownloadHandler()
    {
        return downloadHandler;
    }

    public UploadHandler getUploadHandler()
    {
        return uploadHandler;
    }

    public ListingHandler getListingHandler()
    {
        return listingHandler;
    }

    public ExistenceHandler getExistenceHandler()
    {
        return existenceHandler;
    }

    public XMLInfrastructure getXml()
    {
        return xml;
    }

    public XPathManager getXpath()
    {
        return xpath;
    }

    public MavenPomReader getPomReader()
    {
        return pomReader;
    }

    public MetadataScannerSupport getScannerSupport()
    {
        return scannerSupport;
    }

    public DefaultCartoDataManager getDatabase()
    {
        return database;
    }

    public CartographerBuilder withVersionResolver( final VersionResolver versionResolver )
    {
        this.versionResolver = versionResolver;
        return this;
    }

    public CartographerBuilder withMetadataReader( final MavenMetadataReader metadataReader )
    {
        this.metadataReader = metadataReader;
        return this;
    }

    public CartographerBuilder withArtifactMetadataManager( final ArtifactMetadataManager artifactMetadataManager )
    {
        this.artifactMetadataManager = artifactMetadataManager;
        return this;
    }

    public CartographerBuilder withHttp( final HttpImpl http )
    {
        this.http = http;
        return this;
    }

    public CartographerBuilder withGlobalHttpConfig( final GlobalHttpConfiguration globalHttpConfig )
    {
        this.globalHttpConfig = globalHttpConfig;
        return this;
    }

    public CartographerBuilder withGraphs( final EGraphManager graphs )
    {
        this.graphs = graphs;
        return this;
    }

    public CartographerBuilder withEvents( final NoOpCartoEventManager events )
    {
        this.events = events;
        return this;
    }

    public CartographerBuilder withWsHolder( final GraphWorkspaceHolder wsHolder )
    {
        this.wsHolder = wsHolder;
        return this;
    }

    public CartographerBuilder withFileEvents( final NoOpFileEventManager fileEvents )
    {
        this.fileEvents = fileEvents;
        return this;
    }

    public CartographerBuilder withCache( final FileCacheProvider cache )
    {
        this.cache = cache;
        return this;
    }

    public CartographerBuilder withNfc( final MemoryNotFoundCache nfc )
    {
        this.nfc = nfc;
        return this;
    }

    public CartographerBuilder withAggregatorExecutor( final ScheduledExecutorService aggregatorExecutor )
    {
        this.aggregatorExecutor = aggregatorExecutor;
        return this;
    }

    public CartographerBuilder withTransportExecutor( final ScheduledExecutorService transportExecutor )
    {
        this.transportExecutor = transportExecutor;
        return this;
    }

    public CartographerBuilder withBatchExecutor( final ScheduledExecutorService batchExecutor )
    {
        this.batchExecutor = batchExecutor;
        return this;
    }

    public CartographerBuilder withResolveExecutor( final ScheduledExecutorService resolveExecutor )
    {
        this.resolveExecutor = resolveExecutor;
        return this;
    }

    public CartographerBuilder withDownloadHandler( final DownloadHandler downloadHandler )
    {
        this.downloadHandler = downloadHandler;
        return this;
    }

    public CartographerBuilder withUploadHandler( final UploadHandler uploadHandler )
    {
        this.uploadHandler = uploadHandler;
        return this;
    }

    public CartographerBuilder withListingHandler( final ListingHandler listingHandler )
    {
        this.listingHandler = listingHandler;
        return this;
    }

    public CartographerBuilder withExistenceHandler( final ExistenceHandler existenceHandler )
    {
        this.existenceHandler = existenceHandler;
        return this;
    }

    public CartographerBuilder withXml( final XMLInfrastructure xml )
    {
        this.xml = xml;
        return this;
    }

    public CartographerBuilder withXpath( final XPathManager xpath )
    {
        this.xpath = xpath;
        return this;
    }

    public CartographerBuilder withPomReader( final MavenPomReader pomReader )
    {
        this.pomReader = pomReader;
        return this;
    }

    public CartographerBuilder withScannerSupport( final MetadataScannerSupport scannerSupport )
    {
        this.scannerSupport = scannerSupport;
        return this;
    }

    public CartographerBuilder withPatcherSupport( final PatcherSupport patcherSupport )
    {
        this.patcherSupport = patcherSupport;
        return this;
    }

    public CartographerBuilder withDatabase( final DefaultCartoDataManager database )
    {
        this.database = database;
        return this;
    }

    public Collection<DepgraphPatcher> getDepgraphPatchers()
    {
        return depgraphPatchers;
    }

    public PatcherSupport getPatcherSupport()
    {
        return patcherSupport;
    }
}
