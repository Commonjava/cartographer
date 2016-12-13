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
package org.commonjava.cartographer.graph;

import org.commonjava.cartographer.request.build.GraphDescriptionBuilder;
import org.commonjava.cartographer.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleParentRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.cartographer.request.SingleGraphRequest;
import org.commonjava.cartographer.request.build.SingleGraphRequestBuilder;
import org.commonjava.cartographer.testutil.CartoFixture;
import org.commonjava.cartographer.testutil.GroupIdFilter;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class GraphResolverTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public CartoFixture fixture = new CartoFixture();

    @Test
    public void connectIncompleteWithDiscovery_Idempotency_DepsOnly()
            throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );
        final String baseG = "org.foo";

        final ProjectVersionRef root = new SimpleProjectVersionRef( baseG, "root", "1" );
        final ProjectVersionRef c1 = new SimpleProjectVersionRef( baseG, "child-1", "1.0" );
        final ProjectVersionRef gc1 = new SimpleProjectVersionRef( baseG, "grandchild-1", "1.0" );
        final ProjectVersionRef c2 = new SimpleProjectVersionRef( "org.bar", "child-2", "1.0" );
        final ProjectVersionRef c3 = new SimpleProjectVersionRef( baseG, "child-3", "1.0" );
        final ProjectVersionRef gc3 = new SimpleProjectVersionRef( baseG, "grandchild-3", "1.0" );
        final ProjectVersionRef ggc3 = new SimpleProjectVersionRef( baseG, "great-grandchild-3", "1.0" );

        final RelationshipGraph rootlessGraph =
                fixture.openGraph( new ViewParams( System.currentTimeMillis() + ".db" ), true );

        /* @formatter:off */
        rootlessGraph.storeRelationships( Arrays.<ProjectRelationship<?, ?>>asList(
            new SimpleDependencyRelationship( src, root, c1.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false, false, false ),
            new SimpleDependencyRelationship( src, root, c2.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false, false, false ),
            new SimpleDependencyRelationship( src, root, c3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false, false, false ),
            new SimpleDependencyRelationship( src, c1, gc1.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false, false, false )
        ) );

        fixture.getDiscoverer().mapResult( gc1, new DiscoveryResult(
            src,
            gc1,
            new HashSet<ProjectRelationship<?, ?>>( Collections.singletonList( new SimpleParentRelationship( gc1 ) ) ),
            new HashSet<ProjectRelationship<?, ?>>()
        ) );
        /* @formatter:on */

        final SingleGraphRequest recipe = SingleGraphRequestBuilder.newSingleGraphResolverRecipeBuilder()
                                                                   .withWorkspaceId( rootlessGraph.getWorkspaceId() )
                                                                   .withResolve( true )
                                                                   .withSource( src.toString() )
                                                                   .withGraph(
                                                                           GraphDescriptionBuilder.newGraphDescriptionBuilder()
                                                                                                  .withFilter(
                                                                                                          new GroupIdFilter(
                                                                                                                  baseG ) )
                                                                                                  .withRoots( root )
                                                                                                  .build() )
                                                                   .build();

        fixture.getGraphResolver().resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe, ( graph ) -> {

            Set<ProjectVersionRef> resolved = graph.getRoots();
            assertThat( resolved.contains( root ), equalTo( true ) );

            assertThat( fixture.getDiscoverer().sawDiscovery( gc1 ), equalTo( true ) );
            assertThat( fixture.getDiscoverer().sawDiscovery( c2 ), equalTo( false ) );

            logger.info( "\n\n\n\nSECOND PASS\n\n\n\n" );

            try
            {
                graph.storeRelationships( Arrays.<ProjectRelationship<?, ?>>asList(
                        new SimpleDependencyRelationship( src, c3, gc3.asArtifactRef( "jar", null ), DependencyScope.compile,
                                                    0, false, false, false ),
                        new SimpleDependencyRelationship( src, gc3, ggc3.asArtifactRef( "jar", null ),
                                                    DependencyScope.compile, 0, false, false, false ) ) );
            }
            catch ( final Exception e )
            {
                e.printStackTrace();
                Assert.fail( "Failed to store new graph relationships." );
            }

            resolved = graph.getRoots();

            assertThat( resolved.contains( root ), equalTo( true ) );

            assertThat( fixture.getDiscoverer().sawDiscovery( gc1 ), equalTo( true ) );
            assertThat( fixture.getDiscoverer().sawDiscovery( c2 ), equalTo( false ) );
            assertThat( fixture.getDiscoverer().sawDiscovery( gc3 ), equalTo( false ) );
        } );
    }

}
