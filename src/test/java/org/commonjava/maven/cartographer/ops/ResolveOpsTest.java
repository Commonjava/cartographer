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
package org.commonjava.maven.cartographer.ops;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.cartographer.preset.SOBBuildablesFilter;
import org.commonjava.maven.cartographer.testutil.CartoFixture;
import org.commonjava.maven.cartographer.testutil.GroupIdFilter;
import org.commonjava.maven.galley.maven.util.ArtifactPathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.testing.core.transport.job.TestExistence;
import org.commonjava.util.logging.Log4jUtil;
import org.commonjava.util.logging.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ResolveOpsTest
{

    private final Logger logger = new Logger( getClass() );

    @Rule
    public CartoFixture fixture = new CartoFixture( new CoreFixture() );

    private GraphWorkspace ws;

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Before
    public void setup()
        throws Exception
    {
        fixture.initMissingComponents();
        ws = fixture.getData()
                    .createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
    }

    @Test
    public void resolveRepoContent_NoExtras_RuntimePreset_PreResolved_IncludeDirectAncestry()
        throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );

        final LinkedList<ProjectVersionRef> lineage = new LinkedList<ProjectVersionRef>();
        lineage.add( new ProjectVersionRef( "group.id", "my-project", "1.0" ) );
        lineage.add( new ProjectVersionRef( "group.id", "parent-level1", "1" ) );
        lineage.add( new ProjectVersionRef( "group.id", "parent-level2", "1" ) );
        lineage.add( new ProjectVersionRef( "group.id", "parent-level3", "1" ) );
        lineage.add( new ProjectVersionRef( "group.id", "parent-level4", "1" ) );
        lineage.add( new ProjectVersionRef( "group.id", "root", "1" ) );

        final ProjectVersionRef recipeRoot = lineage.getFirst();

        final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();

        final Location location = new SimpleLocation( "test", src.toString(), false, true, true, false, true, 10 );

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
                rels.add( new ParentRelationship( src, last, ref ) );
            }
            last = ref;
        }

        rels.add( new ParentRelationship( src, lineage.getLast() ) );

        final Set<ProjectRelationship<?>> rejects = fixture.getData()
                                                           .storeRelationships( rels );

        System.out.println( "Rejected: " + rejects );
        assertThat( rejects.isEmpty(), equalTo( true ) );

        final RepositoryContentRecipe recipe = new RepositoryContentRecipe();

        recipe.setGraphComposition( new GraphComposition( null,
                                                          Collections.singletonList( new GraphDescription( new SOBBuildablesFilter(),
                                                                                                           Collections.singleton( recipeRoot ) ) ) ) );

        recipe.setResolve( false );
        recipe.setSourceLocation( location );
        recipe.setWorkspaceId( ws.getId() );

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = fixture.getResolveOps()
                                                                                           .resolveRepositoryContents( recipe );
        for ( final ProjectVersionRef ref : lineage )
        {
            assertThat( ref + " not present in repository contents!", contents.containsKey( ref ), equalTo( true ) );
        }
    }

    @Test
    public void connectIncompleteWithDiscovery_Idempotency_DepsOnly()
        throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );
        final String baseG = "org.foo";

        final ProjectVersionRef root = new ProjectVersionRef( baseG, "root", "1" );
        final ProjectVersionRef c1 = new ProjectVersionRef( baseG, "child-1", "1.0" );
        final ProjectVersionRef gc1 = new ProjectVersionRef( baseG + ".child", "grandchild-1", "1.0" );
        final ProjectVersionRef c2 = new ProjectVersionRef( "org.bar", "child-2", "1.0" );
        final ProjectVersionRef c3 = new ProjectVersionRef( baseG, "child-3", "1.0" );
        final ProjectVersionRef gc3 = new ProjectVersionRef( baseG, "grandchild-3", "1.0" );
        final ProjectVersionRef ggc3 = new ProjectVersionRef( baseG, "great-grandchild-3", "1.0" );

        /* @formatter:off */
        fixture.getData().storeRelationships( Arrays.<ProjectRelationship<?>>asList(
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

        List<ProjectVersionRef> resolved = fixture.getResolveOps()
                                                  .resolve( src.toString(), options, root );
        assertThat( resolved.contains( root ), equalTo( true ) );

        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( gc1 ), equalTo( true ) );
        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( c2 ), equalTo( false ) );

        logger.info( "\n\n\n\nSECOND PASS\n\n\n\n" );

        /* @formatter:off */
        fixture.getData().storeRelationships( Arrays.<ProjectRelationship<?>>asList( 
            new DependencyRelationship( src, c3, gc3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false ),
            new DependencyRelationship( src, gc3, ggc3.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false )
        ) );
        /* @formatter:on */

        resolved = fixture.getResolveOps()
                          .resolve( src.toString(), options, root );
        assertThat( resolved.contains( root ), equalTo( true ) );

        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( gc1 ), equalTo( true ) );
        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( c2 ), equalTo( false ) );
        assertThat( fixture.getDiscoverer()
                           .sawDiscovery( gc3 ), equalTo( false ) );
    }

}
