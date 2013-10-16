package org.commonjava.maven.cartographer.util;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.testutil.CartoFixture;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.PluginDependencyView;
import org.commonjava.maven.galley.maven.model.view.PluginView;
import org.commonjava.maven.galley.maven.util.ArtifactPathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.testing.core.transport.job.TestDownload;
import org.commonjava.util.logging.Log4jUtil;
import org.commonjava.util.logging.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class MavenModelProcessorTest
{

    private static final String PROJ_BASE = "pom-processor/";

    private final Logger logger = new Logger( getClass() );

    @Rule
    public CartoFixture fixture = new CartoFixture( new CoreFixture() );

    @BeforeClass
    public static void startLogging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Before
    public void setup()
    {
        fixture.initMissingComponents();
    }

    @Test
    public void resolvePluginVersionFromManagementExpression()
        throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );

        final ProjectVersionRef childRef = new ProjectVersionRef( "org.test", "test-child", "1.0" );

        final LinkedHashMap<ProjectVersionRef, String> lineage = new LinkedHashMap<>();
        lineage.put( childRef, "child.pom.xml" );
        lineage.put( new ProjectVersionRef( "org.test", "test-parent", "1.0" ), "parent.pom.xml" );

        final Location location = new SimpleLocation( "test", src.toString(), false, true, true, false, true, 10 );

        final String base = PROJ_BASE + "version-expression-managed-parent-plugin/";

        for ( final Entry<ProjectVersionRef, String> entry : lineage.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final String filename = entry.getValue();

            final String path = ArtifactPathUtils.formatArtifactPath( ref.asPomArtifact(), fixture.getMapper() );

            fixture.getTransport()
                   .registerDownload( new ConcreteResource( location, path ), new TestDownload( base + filename ) );
        }

        final Transfer transfer = fixture.getArtifacts()
                                         .retrieve( location, childRef.asPomArtifact() );

        final MavenPomView pomView = fixture.getPomReader()
                                            .read( transfer, Collections.singletonList( location ) );

        final List<PluginView> buildPlugins = pomView.getAllBuildPlugins();

        assertThat( buildPlugins, notNullValue() );
        assertThat( buildPlugins.size(), equalTo( 1 ) );

        final PluginView pv = buildPlugins.get( 0 );
        assertThat( pv, notNullValue() );
        assertThat( pv.getVersion(), equalTo( "1.0" ) );

        final DiscoveryResult result = fixture.getModelProcessor()
                                              .readRelationships( pomView, src );

        final Set<ProjectRelationship<?>> rels = result.getAcceptedRelationships();

        logger.info( "Found %d relationships:\n\n  %s", rels.size(), join( rels, "\n  " ) );

        boolean seen = false;
        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( rel.getType() == RelationshipType.PLUGIN && !rel.isManaged() )
            {
                if ( seen )
                {
                    fail( "Multiple plugins found!" );
                }

                seen = true;
                assertThat( rel.getTarget()
                               .getVersionString(), equalTo( "1.0" ) );
            }
        }

        if ( !seen )
        {
            fail( "Plugin relationship not found!" );
        }
    }

    @Test
    public void resolvePluginDependencyFromManagedInfo()
        throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );

        final ProjectVersionRef childRef = new ProjectVersionRef( "org.test", "test-child", "1.0" );

        final LinkedHashMap<ProjectVersionRef, String> lineage = new LinkedHashMap<>();
        lineage.put( childRef, "child.pom.xml" );
        lineage.put( new ProjectVersionRef( "org.test", "test-parent", "1.0" ), "parent.pom.xml" );

        final Location location = new SimpleLocation( "test", src.toString(), false, true, true, false, true, 10 );

        final String base = PROJ_BASE + "dependency-in-managed-parent-plugin/";

        for ( final Entry<ProjectVersionRef, String> entry : lineage.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final String filename = entry.getValue();

            final String path = ArtifactPathUtils.formatArtifactPath( ref.asPomArtifact(), fixture.getMapper() );

            fixture.getTransport()
                   .registerDownload( new ConcreteResource( location, path ), new TestDownload( base + filename ) );
        }

        final Transfer transfer = fixture.getArtifacts()
                                         .retrieve( location, childRef.asPomArtifact() );

        final MavenPomView pomView = fixture.getPomReader()
                                            .read( transfer, Collections.singletonList( location ) );

        final List<PluginView> buildPlugins = pomView.getAllBuildPlugins();

        assertThat( buildPlugins, notNullValue() );
        assertThat( buildPlugins.size(), equalTo( 1 ) );

        final PluginView pv = buildPlugins.get( 0 );
        assertThat( pv, notNullValue() );

        final List<PluginDependencyView> deps = pv.getLocalPluginDependencies();
        assertThat( deps, notNullValue() );
        assertThat( deps.size(), equalTo( 1 ) );

        final PluginDependencyView pdv = deps.get( 0 );
        assertThat( pdv, notNullValue() );
        assertThat( pdv.asArtifactRef()
                       .getVersionString(), equalTo( "1.0" ) );

        final DiscoveryResult result = fixture.getModelProcessor()
                                              .readRelationships( pomView, src );
        final Set<ProjectRelationship<?>> rels = result.getAcceptedRelationships();

        logger.info( "Found %d relationships:\n\n  %s", rels.size(), join( rels, "\n  " ) );

        boolean seen = false;
        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( rel.getType() == RelationshipType.PLUGIN_DEP && !rel.isManaged() )
            {
                if ( seen )
                {
                    fail( "Multiple plugin dependencies found!" );
                }

                seen = true;
                assertThat( rel.getTarget()
                               .getVersionString(), equalTo( "1.0" ) );
            }
        }

        if ( !seen )
        {
            fail( "Plugin-dependency relationship not found!" );
        }
    }

}
