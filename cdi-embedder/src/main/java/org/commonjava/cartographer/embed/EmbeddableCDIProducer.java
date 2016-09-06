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
package org.commonjava.cartographer.embed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.cartographer.INTERNAL.graph.discover.DiscovererImpl;
import org.commonjava.cartographer.INTERNAL.graph.discover.SourceManagerImpl;
import org.commonjava.cartographer.ObjectMapperModuleSet;
import org.commonjava.cartographer.conf.CartographerConfig;
import org.commonjava.cartographer.graph.discover.meta.MetadataScannerSupport;
import org.commonjava.cartographer.graph.discover.patch.PatcherSupport;
import org.commonjava.cartographer.graph.mutator.ManagedDependencyGraphMutatorFactory;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.cache.CacheProviderFactory;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProviderConfig;
import org.commonjava.maven.galley.filearc.FileTransportConfig;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.rel.MavenModelProcessor;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by jdcasey on 9/14/15.
 */
@ApplicationScoped
public class EmbeddableCDIProducer
{

    @Inject
    private CartographerConfig config;

    @Inject
    private Instance<ObjectMapperModuleSet> moduleSetInstances;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private MetadataScannerSupport metadataScanners;

    @Inject
    private PatcherSupport patcherSupport;

    @Inject
    private ArtifactManager artifactManager;

    @Inject
    private MavenPomReader pomReader;

    @Inject
    private PathGenerator pathGenerator;

    @Inject
    private TransferDecorator transferDecorator;

    @Inject
    private FileEventManager fileEventManager;

    @Inject
    private CacheProviderFactory cacheProviderFactory;

    private RelationshipGraphFactory graphFactory;

    private SourceManagerImpl sourceManager;

    private DiscovererImpl discoverer;

    private FileTransportConfig fileTransportConfig;

    private GlobalHttpConfiguration globalHttpConfiguration;

    private ManagedDependencyGraphMutatorFactory mutatorFactory;

    private CacheProvider cacheProvider;

    @PostConstruct
    public void postConstruct()
    {
        graphFactory = new RelationshipGraphFactory( new FileNeo4jConnectionFactory( config.getDataBasedir(), false ));
        sourceManager = new SourceManagerImpl();

        discoverer = new DiscovererImpl( new MavenModelProcessor(), pomReader, artifactManager, patcherSupport, metadataScanners );

        if ( moduleSetInstances != null )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Adding modules to ObjectMapper in Cartographer: {}", objectMapper );
            moduleSetInstances.forEach( ( moduleSet ) -> moduleSet.getSerializerModules()
                                                                  .forEach( ( module ) -> objectMapper.registerModule(
                                                                          module ) ) );
        }

        fileTransportConfig = new FileTransportConfig( config.getCacheBasedir(), pathGenerator );
        globalHttpConfiguration = new GlobalHttpConfiguration();
        mutatorFactory = new ManagedDependencyGraphMutatorFactory();
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
    public SourceManagerImpl getSourceManager()
    {
        return sourceManager;
    }

    @Default
    @Produces
    public DiscovererImpl getDiscoverer()
    {
        return discoverer;
    }

    @Produces
    @Default
    public FileTransportConfig getFileTransportConfig()
    {
        return fileTransportConfig;
    }

    @Produces
    @Default
    public GlobalHttpConfiguration getGlobalHttpConfiguration()
    {
        return globalHttpConfiguration;
    }

    @Produces
    @Default
    public ManagedDependencyGraphMutatorFactory getMutatorFactory()
    {
        return mutatorFactory;
    }

    @Produces
    @Default
    public synchronized CacheProvider getCacheProvider()
    {
        if ( cacheProvider == null )
        {
            try
            {
                cacheProvider = cacheProviderFactory.create( pathGenerator, transferDecorator, fileEventManager );
            }
            catch ( GalleyInitException e )
            {
                throw new IllegalStateException( "Failed to create CacheProvider: " + e.getMessage(), e );
            }
        }

        return cacheProvider;
    }
}
