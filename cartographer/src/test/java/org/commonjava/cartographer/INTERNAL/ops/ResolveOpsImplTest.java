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
package org.commonjava.cartographer.INTERNAL.ops;

import org.commonjava.cartographer.testutil.CartoFixture;
import org.commonjava.cartographer.graph.RelationshipGraph;
import org.commonjava.cartographer.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleParentRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.graph.preset.ScopeWithEmbeddedProjectsFilter;
import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.GraphDescription;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.galley.maven.util.ArtifactPathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.testing.core.transport.job.TestExistence;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ResolveOpsImplTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public CartoFixture fixture = new CartoFixture();

    @Test
    public void resolveRepoContent_NoExtras_RuntimePreset_PreResolved_IncludeDirectAncestry()
        throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );

        final LinkedList<ProjectVersionRef> lineage = new LinkedList<ProjectVersionRef>();
        lineage.add( new SimpleProjectVersionRef( "group.id", "my-project", "1.0" ) );
        lineage.add( new SimpleProjectVersionRef( "group.id", "parent-level1", "1" ) );
        lineage.add( new SimpleProjectVersionRef( "group.id", "parent-level2", "1" ) );
        lineage.add( new SimpleProjectVersionRef( "group.id", "parent-level3", "1" ) );
        lineage.add( new SimpleProjectVersionRef( "group.id", "parent-level4", "1" ) );
        lineage.add( new SimpleProjectVersionRef( "group.id", "root", "1" ) );

        final ProjectVersionRef recipeRoot = lineage.getFirst();

        final List<ProjectRelationship<?, ?>> rels = new ArrayList<ProjectRelationship<?, ?>>();

        final Location location = new SimpleLocation( "test", src.toString(), false, true, true, false, true );

        ProjectVersionRef last = null;
        for ( final ProjectVersionRef ref : lineage )
        {
            final String path = ArtifactPathUtils.formatArtifactPath( ref.asPomArtifact(), fixture.getMapper() );

            fixture.getTransport()
                   .registerExistence( new ConcreteResource( location, path ), new TestExistence( true ) );

            fixture.getTransport()
                   .registerExistence( new ConcreteResource( location, path + ".asc" ), new TestExistence( true ) );

            fixture.getTransport()
                   .registerExistence( new ConcreteResource( location, path + ".md5" ), new TestExistence( true ) );

            fixture.getTransport()
                   .registerExistence( new ConcreteResource( location, path + ".sha1" ), new TestExistence( true ) );

            if ( last != null )
            {
                rels.add( new SimpleParentRelationship( src, last, ref ) );
            }
            last = ref;
        }

        rels.add( new SimpleParentRelationship( lineage.getLast() ) );

        final RelationshipGraph rootlessGraph =
            fixture.openGraph( new ViewParams( System.currentTimeMillis() + ".db" ), true );

        final Set<ProjectRelationship<?, ?>> rejects = rootlessGraph.storeRelationships( rels );

        System.out.println( "Rejected: " + rejects );
        assertThat( rejects.isEmpty(), equalTo( true ) );

        final RepositoryContentRequest recipe = new RepositoryContentRequest();

        recipe.setGraphComposition( new GraphComposition(
                                                          null,
                                                          Collections.singletonList( new GraphDescription(
                                                                                                           new ScopeWithEmbeddedProjectsFilter(
                                                                                                                                                DependencyScope.runtime,
                                                                                                                                                false ),
                                                                                                           null,
                                                                                                           Collections.singleton( recipeRoot ) ) ) ) );

        recipe.setResolve( false );
        recipe.setSourceLocation( location );
        recipe.setWorkspaceId( rootlessGraph.getWorkspaceId() );

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents =
            fixture.getResolveOps()
                   .resolveRepositoryContents( recipe );
        for ( final ProjectVersionRef ref : lineage )
        {
            assertThat( ref + " not present in repository contents!", contents.containsKey( ref ), equalTo( true ) );
        }
    }

}
