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
package org.commonjava.maven.cartographer.agg;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.testutil.CartoFixture;
import org.commonjava.maven.cartographer.testutil.GroupIdFilter;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultGraphAggregatorTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public CartoFixture fixture = new CartoFixture( new CoreFixture() );

    @Before
    public void setup()
        throws Exception
    {
        fixture.initMissingComponents();
    }

    @Test
    public void connectIncompleteWithDiscovery_Idempotency_DepsOnly()
        throws Exception
    {
        final URI src = new URI( "test:source" );
        final String baseG = "org.foo";

        final ProjectVersionRef root = new ProjectVersionRef( baseG, "root", "1" );
        final ProjectVersionRef c1 = new ProjectVersionRef( baseG, "child-1", "1.0" );
        //        final ProjectVersionRef gc1 = new ProjectVersionRef( baseG + ".child", "grandchild-1", "1.0" );
        final ProjectVersionRef gc1 = new ProjectVersionRef( baseG, "grandchild-1", "1.0" );
        final ProjectVersionRef c2 = new ProjectVersionRef( "org.bar", "child-2", "1.0" );
        final ProjectVersionRef c3 = new ProjectVersionRef( baseG, "child-3", "1.0" );
        final ProjectVersionRef gc3 = new ProjectVersionRef( baseG, "grandchild-3", "1.0" );
        final ProjectVersionRef ggc3 = new ProjectVersionRef( baseG, "great-grandchild-3", "1.0" );

        final RelationshipGraph rootless =
            fixture.openGraph( new ViewParams( System.currentTimeMillis() + ".db" ), true );
        rootless.addActiveSource( src );

        /* @formatter:off */
        rootless.storeRelationships( Arrays.<ProjectRelationship<?>>asList(
            new DependencyRelationship( src, root, c1.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, root, c2.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, root, c3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, c1, gc1.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false )
        ) );
        
        fixture.getDiscoverer().mapResult( gc1, new DiscoveryResult( 
            src,
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

        final RelationshipGraph graph =
            fixture.openGraph( new ViewParams.Builder( rootless.getParams() ).withFilter( new GroupIdFilter( baseG ) )
                                                                             .withRoots( root )
                                                                             .build(), false );
        assertThat( graph, notNullValue() );

        fixture.getAggregator()
               .connectIncomplete( graph, options );

        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( gc1 ), equalTo( true ) );

        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( c2 ), equalTo( false ) );

        logger.info( "\n\n\n\nSECOND PASS\n\n\n\n" );

        /* @formatter:off */
        rootless.storeRelationships( Arrays.<ProjectRelationship<?>>asList( 
            new DependencyRelationship( src, c3, gc3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, gc3, ggc3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false )
        ) );
        /* @formatter:on */

        fixture.getAggregator()
               .connectIncomplete( graph, options );

        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( gc1 ), equalTo( true ) );

        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( c2 ), equalTo( false ) );

        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( gc3 ), equalTo( false ) );
    }
}
