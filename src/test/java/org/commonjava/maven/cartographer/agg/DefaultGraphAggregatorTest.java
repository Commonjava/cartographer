package org.commonjava.maven.cartographer.agg;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Executors;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4JEGraphDriver;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.event.NoOpCartoEventManager;
import org.commonjava.maven.cartographer.testutil.TestAggregatorDiscoverer;
import org.commonjava.util.logging.Log4jUtil;
import org.commonjava.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultGraphAggregatorTest
{

    public class GroupIdFilter
        implements ProjectRelationshipFilter
    {

        private final String groupId;

        public GroupIdFilter( final String groupId )
        {
            this.groupId = groupId;
        }

        @Override
        public boolean accept( final ProjectRelationship<?> rel )
        {
            return groupId.equals( rel.getTarget()
                                      .getGroupId() );
        }

        @Override
        public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
        {
            return new GroupIdFilter( groupId + ".child" );
        }

        @Override
        public void render( final StringBuilder sb )
        {
            sb.append( "Artifacts with groupId [" )
              .append( groupId )
              .append( ']' );
        }

    }

    private final Logger logger = new Logger( getClass() );

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private TestAggregatorDiscoverer discoverer;

    private DefaultGraphAggregator aggregator;

    private DefaultCartoDataManager data;

    private EGraphManager graphs;

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Before
    public void setup()
        throws Exception
    {
        graphs = new EGraphManager( new FileNeo4JEGraphDriver( temp.newFolder( "graph.db" ) ) );

        data = new DefaultCartoDataManager( graphs, new GraphWorkspaceHolder(), new NoOpCartoEventManager() );
        data.createTemporaryWorkspace( new GraphWorkspaceConfiguration() );

        discoverer = new TestAggregatorDiscoverer( data );

        aggregator = new DefaultGraphAggregator( data, discoverer, Executors.newFixedThreadPool( 2 ) );
    }

    @After
    public void teardown()
        throws Exception
    {
        graphs.close();
    }

    @Test
    public void connectIncompleteWithDiscovery_Idempotency_DepsOnly()
        throws Exception
    {
        final URI src = new URI( "test:source" );
        final String baseG = "org.foo";

        final ProjectVersionRef root = new ProjectVersionRef( baseG, "root", "1" );
        final ProjectVersionRef c1 = new ProjectVersionRef( baseG, "child-1", "1.0" );
        final ProjectVersionRef gc1 = new ProjectVersionRef( baseG + ".child", "grandchild-1", "1.0" );
        final ProjectVersionRef c2 = new ProjectVersionRef( "org.bar", "child-2", "1.0" );
        final ProjectVersionRef c3 = new ProjectVersionRef( baseG, "child-3", "1.0" );
        final ProjectVersionRef gc3 = new ProjectVersionRef( baseG, "grandchild-3", "1.0" );
        final ProjectVersionRef ggc3 = new ProjectVersionRef( baseG, "great-grandchild-3", "1.0" );

        /* @formatter:off */
        data.storeRelationships( Arrays.<ProjectRelationship<?>>asList(
            new DependencyRelationship( src, root, c1.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, root, c2.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, root, c3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, c1, gc1.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false )
        ) );
        
        discoverer.mapResult( gc1, new DiscoveryResult( 
            gc1,
            new HashSet<ProjectRelationship<?>>( Arrays.asList( new ParentRelationship( src, gc1 ) ) ),
            new HashSet<ProjectRelationship<?>>()
        ) );
        /* @formatter:on */

        final DefaultAggregatorOptions options = new DefaultAggregatorOptions().setDiscoveryEnabled( true )
                                                                               .setDiscoverySource( src )
                                                                               .setFilter( new GroupIdFilter( baseG ) )
                                                                               .setProcessIncompleteSubgraphs( true )
                                                                               .setProcessVariableSubgraphs( true )
                                                                               .setDiscoveryTimeoutMillis( 10 );

        final EProjectWeb web = data.getProjectWeb( options.getFilter(), root );
        assertThat( web, notNullValue() );

        EProjectWeb result = aggregator.connectIncomplete( web, options );
        assertThat( result, notNullValue() );

        assertThat( discoverer.sawDiscovery( gc1 ), equalTo( true ) );
        assertThat( discoverer.sawDiscovery( c2 ), equalTo( false ) );

        logger.info( "\n\n\n\nSECOND PASS\n\n\n\n" );

        /* @formatter:off */
        data.storeRelationships( Arrays.<ProjectRelationship<?>>asList( 
            new DependencyRelationship( src, c3, gc3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, gc3, ggc3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false )
        ) );
        /* @formatter:on */

        result = aggregator.connectIncomplete( web, options );
        assertThat( result, notNullValue() );

        assertThat( discoverer.sawDiscovery( gc1 ), equalTo( true ) );
        assertThat( discoverer.sawDiscovery( c2 ), equalTo( false ) );
        assertThat( discoverer.sawDiscovery( gc3 ), equalTo( false ) );
    }
}
