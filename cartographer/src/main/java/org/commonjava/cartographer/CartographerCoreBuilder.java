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
package org.commonjava.cartographer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.commonjava.cartographer.graph.mutator.ManagedDependencyGraphMutatorFactory;
import org.commonjava.cartographer.graph.mutator.MutatorSelector;
import org.commonjava.cartographer.graph.mutator.NoOpGraphMutatorFactory;
import org.commonjava.cartographer.graph.preset.*;
import org.commonjava.cartographer.ops.*;
import org.commonjava.cdi.util.weft.NamedThreadFactory;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.NeoSpecificProjectRelationshipSerializerModule;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.NeoSpecificProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.cartographer.INTERNAL.graph.agg.DefaultGraphAggregator;
import org.commonjava.cartographer.INTERNAL.ops.*;
import org.commonjava.cartographer.graph.GraphResolver;
import org.commonjava.cartographer.spi.graph.agg.GraphAggregator;
import org.commonjava.cartographer.INTERNAL.graph.discover.DiscovererImpl;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.cartographer.spi.graph.discover.ProjectRelationshipDiscoverer;
import org.commonjava.cartographer.INTERNAL.graph.discover.SourceManagerImpl;
import org.commonjava.cartographer.graph.discover.meta.LicenseScanner;
import org.commonjava.cartographer.graph.discover.meta.MetadataScanner;
import org.commonjava.cartographer.graph.discover.meta.MetadataScannerSupport;
import org.commonjava.cartographer.graph.discover.meta.ScmUrlScanner;
import org.commonjava.cartographer.graph.discover.patch.DepgraphPatcher;
import org.commonjava.cartographer.graph.discover.patch.PatcherSupport;
import org.commonjava.cartographer.event.NoOpCartoEventManager;
import org.commonjava.cartographer.graph.MavenModelProcessor;
import org.commonjava.cartographer.graph.MultiGraphCalculator;
import org.commonjava.cartographer.graph.RecipeResolver;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.filearc.FileTransport;
import org.commonjava.maven.galley.filearc.ZipJarTransport;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.ArtifactMetadataManager;
import org.commonjava.maven.galley.maven.GalleyMaven;
import org.commonjava.maven.galley.maven.GalleyMavenBuilder;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.model.view.XPathManager;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.spi.transport.LocationResolver;
import org.commonjava.maven.galley.spi.transport.Transport;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CartographerCoreBuilder
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private int aggregatorThreads = 2;

    private int resolverThreads = 10;

    private RelationshipGraphFactory graphFactory;

    private Collection<MetadataScanner> metadataScanners;

    private ProjectRelationshipDiscoverer discoverer;

    private NoOpCartoEventManager events;

    private ExecutorService aggregatorExecutor;

    private ExecutorService resolveExecutor;

    private MetadataScannerSupport scannerSupport;

    private Collection<DepgraphPatcher> depgraphPatchers;

    private PatcherSupport patcherSupport;

    private final RelationshipGraphConnectionFactory connectionFactory;

    private GraphAggregator aggregator;

    private DiscoverySourceManager sourceManager;

    private Http http;

    private GlobalHttpConfiguration globalHttpConfig;

    private MavenModelProcessor mavenModelProcessor;

    private RecipeResolver dtoResolver;

    private final GalleyMavenBuilder mavenBuilder;

    private final GalleyMaven maven;

    private PresetSelector presetSelector;

    private ObjectMapper objectMapper;

    private MutatorSelector mutatorSelector;

    public CartographerCoreBuilder( final GalleyMaven galleyMaven,
                                    final RelationshipGraphConnectionFactory connectionFactory )
                    throws CartoDataException
    {
        this.maven = galleyMaven;
        this.mavenBuilder = null;
        this.connectionFactory = connectionFactory;
    }

    public CartographerCoreBuilder( final File resolverCacheDir,
                                    final RelationshipGraphConnectionFactory connectionFactory )
                    throws CartoDataException
    {
        this.maven = null;
        this.mavenBuilder = new GalleyMavenBuilder( resolverCacheDir );
        this.connectionFactory = connectionFactory;
    }

    public CartographerCoreBuilder( final CacheProvider cache,
                                    final RelationshipGraphConnectionFactory connectionFactory )
                    throws CartoDataException
    {
        this.maven = null;
        this.mavenBuilder = new GalleyMavenBuilder( cache );
        this.connectionFactory = connectionFactory;
    }

    public CartographerCoreBuilder initHttpComponents()
    {
        checkMaven();
        if ( mavenBuilder.getPasswordManager() == null )
        {
            mavenBuilder.withPasswordManager( new MemoryPasswordManager() );
        }

        if ( http == null )
        {
            http = new HttpImpl( mavenBuilder.getPasswordManager() );
        }

        if ( globalHttpConfig == null )
        {
            globalHttpConfig = new GlobalHttpConfiguration();
        }

        return this;
    }

    private void checkMaven()
    {
        if ( maven != null )
        {
            throw new IllegalStateException( "Galley Maven components were pre-initiailized!" );
        }
    }

    public CartographerCoreBuilder withGraphAggregator( final GraphAggregator aggregator )
    {
        this.aggregator = aggregator;
        return this;
    }

    public CartographerCoreBuilder withDefaultTransports()
    {
        checkMaven();
        initHttpComponents();
        mavenBuilder.withEnabledTransports( new HttpClientTransport( http, new ObjectMapper(), globalHttpConfig ),
                                            new FileTransport(), new ZipJarTransport() );

        return this;
    }

    public CartographerCoreBuilder withTransport( final Transport transport )
    {
        checkMaven();
        mavenBuilder.withAdditionalTransport( transport );
        return this;
    }

    public CartographerCore build()
                    throws CartoDataException
    {
        // this has implications on how the maven components are built, below...so it has to happen first.
        if ( this.sourceManager == null )
        {
            final SourceManagerImpl smi = new SourceManagerImpl();
            this.sourceManager = smi;
            withLocationExpander( smi );
            withLocationResolver( smi );
        }

        if ( maven == null )
        {
            final List<Transport> transports = mavenBuilder.getEnabledTransports();
            if ( transports == null || transports.isEmpty() )
            {
                withDefaultTransports();
            }

            try
            {
                mavenBuilder.initMissingComponents();
            }
            catch ( final GalleyInitException e )
            {
                throw new CartoDataException( "Failed to initialize missing Galley components: %s", e, e.getMessage() );
            }
        }

        // TODO: This needs to be replaced with a real implementation.
        if ( events == null )
        {
            events = new NoOpCartoEventManager();
        }

        aggregatorThreads = aggregatorThreads < 2 ? 2 : aggregatorThreads;

        if ( aggregatorExecutor == null )
        {
            aggregatorExecutor = Executors.newScheduledThreadPool( aggregatorThreads,
                                                                   new NamedThreadFactory( "carto-aggregator", true,
                                                                                           8 ) );
        }

        resolverThreads = resolverThreads < aggregatorThreads ? 5 * aggregatorThreads : resolverThreads;

        if ( resolveExecutor == null )
        {
            resolveExecutor = Executors.newScheduledThreadPool( resolverThreads,
                                                                new NamedThreadFactory( "carto-graph", true, 8 ) );
        }

        if ( this.metadataScanners == null )
        {
            this.metadataScanners = new ArrayList<MetadataScanner>( Arrays.asList( new LicenseScanner( getPomReader() ),
                                                                                   new ScmUrlScanner(
                                                                                                   getPomReader() ) ) );
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
            this.patcherSupport = new PatcherSupport(
                            this.depgraphPatchers.toArray( new DepgraphPatcher[this.depgraphPatchers.size()] ) );
        }

        if ( mavenModelProcessor == null )
        {
            mavenModelProcessor = new MavenModelProcessor();
        }

        if ( this.discoverer == null )
        {
            this.discoverer = new DiscovererImpl( mavenModelProcessor, getPomReader(), getArtifactManager(),
                                                  patcherSupport, scannerSupport );
        }

        if ( aggregator == null )
        {
            aggregator = new DefaultGraphAggregator( discoverer, aggregatorExecutor );
        }

        if ( presetSelector == null )
        {
            presetSelector = new PresetSelector(
                            Arrays.<PresetFactory>asList( new BuildRequirementProjectsFilterFactory(),
                                                          new ScopeWithEmbeddedProjectsFilterFactory(),
                                                          new ScopedProjectFilterFactory() ) );
        }

        if ( mutatorSelector == null )
        {
            mutatorSelector = new MutatorSelector(
                    Arrays.asList( new ManagedDependencyGraphMutatorFactory(), new NoOpGraphMutatorFactory() ) );
        }

        if ( dtoResolver == null )
        {
            dtoResolver = new RecipeResolver( getLocationResolver(), getLocationExpander(), sourceManager,
                                              getPomReader(), presetSelector, mutatorSelector );
        }

        withStandardObjectMapperModules();

        logger.debug( "Object mapper: {}", objectMapper );

        final RelationshipGraphFactory graphFactory = new RelationshipGraphFactory( connectionFactory );

        MultiGraphCalculator calculator = new MultiGraphCalculator( graphFactory );

        GraphResolver resolver =
                        new GraphResolver( calculator, sourceManager, discoverer, aggregator, getArtifactManager(),
                                           resolveExecutor, graphFactory, dtoResolver );

        final CalculationOps calculationOps = new CalculationOpsImpl( calculator, resolver );

        final ResolveOps resolveOps =
                        new ResolveOpsImpl( sourceManager, discoverer, getArtifactManager(), resolveExecutor,
                                            dtoResolver, resolver );

        final GraphOps graphOps = new GraphOpsImpl( resolver );

        final GraphRenderingOps graphRenderingOps =
                        new GraphRenderingOpsImpl( resolveOps, resolver, getLocationExpander(), dtoResolver );

        final MetadataOps metadataOps =
                        new MetadataOpsImpl( getArtifactManager(), getPomReader(), scannerSupport, resolver,
                                             dtoResolver );

        try
        {
            return new CartographerCore( maven == null ? mavenBuilder.build() : maven, calculationOps, graphOps,
                                     graphRenderingOps, metadataOps, resolveOps, graphFactory, resolver, calculator,
                                     objectMapper );
        }
        catch ( final GalleyInitException e )
        {
            throw new CartoDataException( "Failed to build Galley Maven component: %s", e, e.getMessage() );
        }
    }

    public CartographerCoreBuilder withStandardObjectMapperModules()
    {
        initDefaultObjectMapper();

        logger.debug( "{} adding standard modules", objectMapper );
        objectMapper.registerModules( new ProjectVersionRefSerializerModule(),
                                      new ProjectRelationshipSerializerModule(),
                                      new NeoSpecificProjectVersionRefSerializerModule(),
                                      new NeoSpecificProjectRelationshipSerializerModule() );
        return this;
    }

    public CartographerCoreBuilder initDefaultObjectMapper()
    {
        if ( objectMapper == null )
        {
            logger.debug( "initializing object mapper" );
            objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion( Include.NON_NULL );
            objectMapper.enable( SerializationFeature.INDENT_OUTPUT );
            objectMapper.disable( SerializationFeature.WRITE_NULL_MAP_VALUES );
            objectMapper.disable( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );
            objectMapper.enable( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS );
        }

        return this;
    }

    public CartographerCoreBuilder withObjectMapperModules( final Module... modules )
    {
        initDefaultObjectMapper();

        logger.debug( "{} adding custom modules", objectMapper );
        objectMapper.registerModules( modules );
        return this;
    }

    public MavenModelProcessor getMavenModelProcessor()
    {
        return mavenModelProcessor;
    }

    public CartographerCoreBuilder withMavenModelProcessor( final MavenModelProcessor mmp )
    {
        this.mavenModelProcessor = mmp;
        return this;
    }

    public RelationshipGraphFactory getGraphFactory()
    {
        return graphFactory;
    }

    public int getAggregatorThreads()
    {
        return aggregatorThreads;
    }

    public int getResolverThreads()
    {
        return resolverThreads;
    }

    public DiscoverySourceManager getSourceManager()
    {
        return sourceManager;
    }

    public Collection<MetadataScanner> getMetadataScanners()
    {
        return metadataScanners;
    }

    public ProjectRelationshipDiscoverer getDiscoverer()
    {
        return discoverer;
    }

    public CartographerCoreBuilder withGraphFactory( final RelationshipGraphFactory graphFactory )
    {
        this.graphFactory = graphFactory;
        return this;
    }

    public CartographerCoreBuilder withAggregatorThreads( final int resolverThreads )
    {
        this.aggregatorThreads = resolverThreads;
        return this;
    }

    public CartographerCoreBuilder withResolverThreads( final int resolverThreads )
    {
        this.resolverThreads = resolverThreads;
        return this;
    }

    public CartographerCoreBuilder withSourceManager( final DiscoverySourceManager sourceManager )
    {
        this.sourceManager = sourceManager;
        if ( sourceManager instanceof LocationResolver )
        {
            withLocationResolver( (LocationResolver) sourceManager );
        }

        if ( sourceManager instanceof LocationExpander )
        {
            withLocationExpander( (LocationExpander) sourceManager );
        }

        return this;
    }

    public CartographerCoreBuilder withMetadataScanners( final Collection<MetadataScanner> metadataScanners )
    {
        this.metadataScanners = metadataScanners;
        return this;
    }

    public CartographerCoreBuilder withDepgraphPatchers( final Collection<DepgraphPatcher> patchers )
    {
        this.depgraphPatchers = patchers;
        return this;
    }

    public CartographerCoreBuilder withDiscoverer( final ProjectRelationshipDiscoverer discoverer )
    {
        this.discoverer = discoverer;
        return this;
    }

    public Http getHttp()
    {
        return http;
    }

    public GlobalHttpConfiguration getGlobalHttpConfig()
    {
        return globalHttpConfig;
    }

    public NoOpCartoEventManager getCartoEvents()
    {
        return events;
    }

    public ExecutorService getAggregatorExecutor()
    {
        return aggregatorExecutor;
    }

    public ExecutorService getResolveExecutor()
    {
        return resolveExecutor;
    }

    public MetadataScannerSupport getScannerSupport()
    {
        return scannerSupport;
    }

    public CartographerCoreBuilder withHttp( final Http http )
    {
        this.http = http;
        return this;
    }

    public CartographerCoreBuilder withGlobalHttpConfig( final GlobalHttpConfiguration globalHttpConfig )
    {
        this.globalHttpConfig = globalHttpConfig;
        return this;
    }

    public CartographerCoreBuilder withCartoEvents( final NoOpCartoEventManager events )
    {
        this.events = events;
        return this;
    }

    public CartographerCoreBuilder withAggregatorExecutor( final ScheduledExecutorService aggregatorExecutor )
    {
        this.aggregatorExecutor = aggregatorExecutor;
        return this;
    }

    public CartographerCoreBuilder withResolveExecutor( final ScheduledExecutorService resolveExecutor )
    {
        this.resolveExecutor = resolveExecutor;
        return this;
    }

    public CartographerCoreBuilder withScannerSupport( final MetadataScannerSupport scannerSupport )
    {
        this.scannerSupport = scannerSupport;
        return this;
    }

    public CartographerCoreBuilder withPatcherSupport( final PatcherSupport patcherSupport )
    {
        this.patcherSupport = patcherSupport;
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

    public GraphAggregator getGraphAggregator()
    {
        return aggregator;
    }

    public ArtifactManager getArtifactManager()
    {
        return maven == null ? mavenBuilder.getArtifactManager() : maven.getArtifactManager();
    }

    public ArtifactMetadataManager getArtifactMetadataManager()
    {
        return maven == null ? mavenBuilder.getArtifactMetadataManager() : maven.getArtifactMetadataManager();
    }

    public CartographerCoreBuilder withArtifactManager( final ArtifactManager artifactManager )
    {
        checkMaven();
        mavenBuilder.withArtifactManager( artifactManager );
        return this;
    }

    public CartographerCoreBuilder withArtifactMetadataManager( final ArtifactMetadataManager metadata )
    {
        checkMaven();
        mavenBuilder.withArtifactMetadataManager( metadata );
        return this;
    }

    public TypeMapper getTypeMapper()
    {
        return maven == null ? mavenBuilder.getTypeMapper() : maven.getTypeMapper();
    }

    public MavenPomReader getPomReader()
    {
        return maven == null ? mavenBuilder.getPomReader() : maven.getPomReader();
    }

    public CartographerCoreBuilder withPomReader( final MavenPomReader pomReader )
    {
        checkMaven();
        mavenBuilder.withPomReader( pomReader );
        return this;
    }

    public MavenPluginDefaults getPluginDefaults()
    {
        return maven == null ? mavenBuilder.getPluginDefaults() : maven.getPluginDefaults();
    }

    public CartographerCoreBuilder withPluginDefaults( final StandardMaven304PluginDefaults pluginDefaults )
    {
        checkMaven();
        mavenBuilder.withPluginDefaults( pluginDefaults );
        return this;
    }

    public XPathManager getXPathManager()
    {
        return maven == null ? mavenBuilder.getXPathManager() : maven.getXPathManager();
    }

    public CartographerCoreBuilder withXPathManager( final XPathManager xpathManager )
    {
        checkMaven();
        mavenBuilder.withXPathManager( xpathManager );
        return this;
    }

    public XMLInfrastructure getXmlInfrastructure()
    {
        return maven == null ? mavenBuilder.getXmlInfrastructure() : maven.getXmlInfrastructure();
    }

    public CartographerCoreBuilder withXmlInfrastructure( final XMLInfrastructure xmlInfra )
    {
        checkMaven();
        mavenBuilder.withXmlInfrastructure( xmlInfra );
        return this;
    }

    public CartographerCoreBuilder withTypeMapper( final TypeMapper mapper )
    {
        checkMaven();
        mavenBuilder.withTypeMapper( mapper );
        return this;
    }

    public CartographerCoreBuilder withPluginDefaults( final MavenPluginDefaults pluginDefaults )
    {
        checkMaven();
        mavenBuilder.withPluginDefaults( pluginDefaults );
        return this;
    }

    public MavenPluginImplications getPluginImplications()
    {
        return maven == null ? mavenBuilder.getPluginImplications() : maven.getPluginImplications();
    }

    public CartographerCoreBuilder withPluginImplications( final MavenPluginImplications pluginImplications )
    {
        checkMaven();
        mavenBuilder.withPluginImplications( pluginImplications );
        return this;
    }

    public MavenMetadataReader getMavenMetadataReader()
    {
        return maven == null ? mavenBuilder.getMavenMetadataReader() : maven.getMavenMetadataReader();
    }

    public VersionResolver getVersionResolver()
    {
        return maven == null ? mavenBuilder.getVersionResolver() : maven.getVersionResolver();
    }

    public CartographerCoreBuilder withMavenMetadataReader( final MavenMetadataReader metaReader )
    {
        checkMaven();
        mavenBuilder.withMavenMetadataReader( metaReader );
        return this;
    }

    public CartographerCoreBuilder withVersionResolver( final VersionResolver versionResolver )
    {
        checkMaven();
        mavenBuilder.withVersionResolver( versionResolver );
        return this;
    }

    public LocationExpander getLocationExpander()
    {
        return maven == null ? mavenBuilder.getLocationExpander() : maven.getLocationExpander();
    }

    public LocationResolver getLocationResolver()
    {
        return maven == null ? mavenBuilder.getLocationResolver() : maven.getLocationResolver();
    }

    public TransferDecorator getTransferDecorator()
    {
        return maven == null ? mavenBuilder.getTransferDecorator() : maven.getTransferDecorator();
    }

    public FileEventManager getFileEvents()
    {
        return maven == null ? mavenBuilder.getFileEvents() : maven.getFileEvents();
    }

    public CacheProvider getCache()
    {
        return maven == null ? mavenBuilder.getCache() : maven.getCache();
    }

    public NotFoundCache getNfc()
    {
        return maven == null ? mavenBuilder.getNfc() : maven.getNfc();
    }

    public CartographerCoreBuilder withLocationExpander( final LocationExpander locationExpander )
    {
        checkMaven();
        logger.debug( "Setting location expander: {}", locationExpander );
        mavenBuilder.withLocationExpander( locationExpander );
        return this;
    }

    public CartographerCoreBuilder withLocationResolver( final LocationResolver locationResolver )
    {
        checkMaven();
        mavenBuilder.withLocationResolver( locationResolver );
        return this;
    }

    public CartographerCoreBuilder withTransferDecorator( final TransferDecorator decorator )
    {
        checkMaven();
        mavenBuilder.withTransferDecorator( decorator );
        return this;
    }

    public CartographerCoreBuilder withFileEvents( final FileEventManager events )
    {
        checkMaven();
        mavenBuilder.withFileEvents( events );
        return this;
    }

    public CartographerCoreBuilder withCache( final CacheProvider cache )
    {
        checkMaven();
        mavenBuilder.withCache( cache );
        return this;
    }

    public CartographerCoreBuilder withNfc( final NotFoundCache nfc )
    {
        checkMaven();
        mavenBuilder.withNfc( nfc );
        return this;
    }

    public TransportManager getTransportManager()
    {
        return maven == null ? mavenBuilder.getTransportManager() : maven.getTransportManager();
    }

    public TransferManager getTransferManager()
    {
        return maven == null ? mavenBuilder.getTransferManager() : maven.getTransferManager();
    }

    public CartographerCoreBuilder withTransportManager( final TransportManager transportManager )
    {
        checkMaven();
        mavenBuilder.withTransportManager( transportManager );
        return this;
    }

    public CartographerCoreBuilder withTransferManager( final TransferManager transferManager )
    {
        checkMaven();
        mavenBuilder.withTransferManager( transferManager );
        return this;
    }

    public List<Transport> getTransports()
    {
        return maven == null ? mavenBuilder.getEnabledTransports() : maven.getEnabledTransports();
    }

    public CartographerCoreBuilder withTransports( final List<Transport> transports )
    {
        checkMaven();
        mavenBuilder.withEnabledTransports( transports );
        return this;
    }

    public CartographerCoreBuilder withTransports( final Transport... transports )
    {
        checkMaven();
        mavenBuilder.withEnabledTransports( transports );
        return this;
    }

    public ExecutorService getHandlerExecutor()
    {
        return maven == null ? mavenBuilder.getHandlerExecutor() : maven.getHandlerExecutor();
    }

    public CartographerCoreBuilder withHandlerExecutor( final ExecutorService handlerExecutor )
    {
        checkMaven();
        mavenBuilder.withHandlerExecutor( handlerExecutor );
        return this;
    }

    public ExecutorService getBatchExecutor()
    {
        return maven == null ? mavenBuilder.getBatchExecutor() : maven.getBatchExecutor();
    }

    public CartographerCoreBuilder withBatchExecutor( final ExecutorService batchExecutor )
    {
        checkMaven();
        mavenBuilder.withBatchExecutor( batchExecutor );
        return this;
    }

    public PasswordManager getPasswordManager()
    {
        return maven == null ? mavenBuilder.getPasswordManager() : maven.getPasswordManager();
    }

    public CartographerCoreBuilder withPasswordManager( final PasswordManager passwordManager )
    {
        checkMaven();
        mavenBuilder.withPasswordManager( passwordManager );
        return this;
    }

    public File getCacheDir()
    {
        checkMaven();
        return mavenBuilder.getCacheDir();
    }

    public CartographerCoreBuilder withCacheDir( final File cacheDir )
    {
        checkMaven();
        mavenBuilder.withCacheDir( cacheDir );
        return this;
    }

    public CartographerCoreBuilder withAdditionalTransport( final Transport transport )
    {
        checkMaven();
        mavenBuilder.withAdditionalTransport( transport );
        return this;
    }

    public RecipeResolver getDtoResolver()
    {
        return dtoResolver;
    }

    public CartographerCoreBuilder withDtoResolver( final RecipeResolver dtoResolver )
    {
        this.dtoResolver = dtoResolver;
        return this;
    }

    public PresetSelector getPresetSelector()
    {
        return presetSelector;
    }

    public CartographerCoreBuilder withPresetSelector( final PresetSelector presetSelector )
    {
        this.presetSelector = presetSelector;
        return this;
    }

    public MutatorSelector getMutatorSelector()
    {
        return mutatorSelector;
    }

    public CartographerCoreBuilder withMutatorSelector( final MutatorSelector mutatorSelector )
    {
        this.mutatorSelector = mutatorSelector;
        return this;
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public CartographerCoreBuilder withObjectMapper( final ObjectMapper objectMapper )
    {
        this.objectMapper = objectMapper;
        return this;
    }
}
