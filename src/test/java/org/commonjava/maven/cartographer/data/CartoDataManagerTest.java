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
package org.commonjava.maven.cartographer.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.discover.DiscovererImpl;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.discover.post.meta.ScmUrlScanner;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
import org.commonjava.maven.cartographer.testutil.TestCartoCoreProvider;
import org.commonjava.maven.cartographer.testutil.TestCartoEventManager;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.TransferManagerImpl;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.internal.xfer.DownloadHandler;
import org.commonjava.maven.galley.internal.xfer.ExistenceHandler;
import org.commonjava.maven.galley.internal.xfer.ListingHandler;
import org.commonjava.maven.galley.internal.xfer.UploadHandler;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
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
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.NoOpLocationExpander;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class CartoDataManagerTest
    extends AbstractCartoDataManagerTest
{

    private CartoDataManager dataManager;

    private DefaultGraphAggregator aggregator;

    private ProjectRelationshipDiscoverer discoverer;

    private TestCartoCoreProvider provider;

    private XMLInfrastructure xml;

    private XPathManager xpath;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private GraphWorkspaceHolder sessionManager;

    private MemoryNotFoundCache nfc;

    @After
    public void teardown()
        throws Exception
    {
        provider.shutdown();
    }

    @Override
    protected void setupComponents()
        throws Exception
    {
        provider = new TestCartoCoreProvider( temp );
        sessionManager = new GraphWorkspaceHolder();

        dataManager = new CartoGraphUtils( provider.getGraphs(), sessionManager, new TestCartoEventManager() );

        final MavenModelProcessor processor = new MavenModelProcessor( dataManager );

        // TODO: Do we need to flesh this out??
        final TransportManager transportManager = new TransportManagerImpl();

        nfc = new MemoryNotFoundCache();

        final CacheProvider cacheProvider =
            new FileCacheProvider( temp.newFolder( "cache" ), new HashedLocationPathGenerator(), provider.getFileEventManager(),
                                   provider.getTransferDecorator() );

        final ExecutorService executor = Executors.newFixedThreadPool( 2 );
        final ExecutorService batchExecutor = Executors.newFixedThreadPool( 2 );
        final DownloadHandler dh = new DownloadHandler( nfc, executor );
        final UploadHandler uh = new UploadHandler( nfc, executor );
        final ListingHandler lh = new ListingHandler( nfc );
        final ExistenceHandler eh = new ExistenceHandler( nfc );

        final TransferManager transferManager =
            new TransferManagerImpl( transportManager, cacheProvider, nfc, provider.getFileEventManager(), dh, uh, lh, eh, batchExecutor );

        xml = new XMLInfrastructure();
        xpath = new XPathManager();

        final LocationExpander locationExpander = new NoOpLocationExpander();

        final ArtifactMetadataManager meta = new ArtifactMetadataManagerImpl( transferManager, locationExpander );
        final MavenMetadataReader mmr = new MavenMetadataReader( xml, locationExpander, meta, xpath );

        final VersionResolver versions = new VersionResolverImpl( mmr );

        final ArtifactManager artifacts = new ArtifactManagerImpl( transferManager, locationExpander, new StandardTypeMapper(), versions );

        final MavenPomReader pomReader =
            new MavenPomReader( xml, locationExpander, artifacts, xpath, new StandardMaven304PluginDefaults(),
                                new StandardMavenPluginImplications( xml ) );

        // TODO: Add some scanners.
        final MetadataScannerSupport scannerSupport = new MetadataScannerSupport( new ScmUrlScanner( pomReader ) );

        discoverer = new DiscovererImpl( processor, pomReader, artifacts, dataManager, new PatcherSupport(), scannerSupport );

        aggregator = new DefaultGraphAggregator( dataManager, discoverer, Executors.newFixedThreadPool( 2 ) );
    }

    @Override
    protected GraphWorkspaceHolder getSessionManager()
    {
        return sessionManager;
    }

    @Override
    protected CartoDataManager getDataManager()
        throws Exception
    {
        return dataManager;
    }

    @Override
    protected GraphAggregator getAggregator()
        throws Exception
    {
        return aggregator;
    }

    public XMLInfrastructure getXML()
    {
        return xml;
    }

    public XPathManager getXPath()
    {
        return xpath;
    }

}
