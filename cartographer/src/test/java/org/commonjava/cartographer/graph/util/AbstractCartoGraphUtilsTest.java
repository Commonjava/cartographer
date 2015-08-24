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
package org.commonjava.cartographer.graph.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.cartographer.spi.graph.agg.GraphAggregator;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractCartoGraphUtilsTest
{

    protected abstract RelationshipGraphFactory getGraphFactory()
        throws Exception;

    protected abstract GraphAggregator getAggregator()
        throws Exception;

    protected abstract void setupComponents()
        throws Exception;

    private URI sourceUri;

    private RelationshipGraph graph;

    @Before
    public void setupSession()
        throws Exception
    {
        setupComponents();
        graph = getGraphFactory().open( new ViewParams( System.currentTimeMillis() + ".db" ), true );
    }

    protected synchronized URI sourceUri()
        throws URISyntaxException
    {
        if ( sourceUri == null )
        {
            sourceUri = new URI( "test:source" );
        }

        return sourceUri;
    }

    @Test
    public void storeAndValidateContains()
        throws Exception
    {
        final ProjectVersionRef r = new SimpleProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef d = new SimpleProjectVersionRef( "org.test", "dep", "1" );
        final EProjectDirectRelationships root =
            new EProjectDirectRelationships.Builder( sourceUri, r ).withDependency( d, null, null, null, false )
                                                                   .build();

        graph.storeRelationships( root.getExactAllRelationships() );

        assertThat( graph.containsGraph( r ), equalTo( true ) );
    }

    @Test
    public void storeParentChildDescendantProjectsAndRetrieveAncestry()
        throws Exception
    {
        final ProjectVersionRef r = new SimpleProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "org.test", "child", "1.0" );

        final EProjectDirectRelationships root = new EProjectDirectRelationships.Builder( sourceUri, r ).build();
        final EProjectDirectRelationships parent =
            new EProjectDirectRelationships.Builder( sourceUri, p ).withParent( r )
                                                                   .build();
        final EProjectDirectRelationships child =
            new EProjectDirectRelationships.Builder( sourceUri, c ).withParent( p )
                                                                   .build();

        graph.storeRelationships( root.getExactAllRelationships() );
        graph.storeRelationships( parent.getExactAllRelationships() );
        graph.storeRelationships( child.getExactAllRelationships() );

        final List<ProjectVersionRef> ancestry = CartoGraphUtils.getAncestry( c, graph );
        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

    @Test
    public void storeProjectAndRetrieveAllRelationshipsInOneGo()
        throws Exception
    {
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );

        final EProjectDirectRelationships.Builder prb = new EProjectDirectRelationships.Builder( sourceUri, p );

        final ProjectVersionRef parent = new SimpleProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        int idx = 0;
        int pidx = 0;
        final DependencyRelationship papi =
            new DependencyRelationship( sourceUri, p, new SimpleArtifactRef( "org.apache.maven", "maven-plugin-api", "3.0.3",
                                                                       null, null, false ), DependencyScope.compile,
                                        idx++, false );
        final DependencyRelationship art =
            new DependencyRelationship( sourceUri, p, new SimpleArtifactRef( "org.apache.maven", "maven-artifact", "3.0.3",
                                                                       null, null, false ), DependencyScope.compile,
                                        idx++, false );
        final PluginRelationship jarp =
            new PluginRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.plugins",
                                                                         "maven-jar-plugin", "2.2" ), pidx++, false );
        final PluginRelationship comp =
            new PluginRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.plugins",
                                                                         "maven-compiler-plugin", "2.3.2" ), pidx++,
                                    false );
        final ExtensionRelationship wag =
            new ExtensionRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.wagon",
                                                                            "wagon-provider-webdav", "1.0" ), 0 );

        prb.withParent( parent );
        prb.withDependencies( papi, art );
        prb.withPlugins( jarp, comp );
        prb.withExtensions( wag );

        final EProjectDirectRelationships rels = prb.build();

        assertThat( rels.getAllRelationships()
                        .size(), equalTo( 6 ) );

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> resulting =
            graph.findDirectRelationshipsFrom( rels.getProjectRef(), false, RelationshipType.values() );

        final Set<ProjectVersionRef> targets = RelationshipUtils.targets( resulting );

        assertThat( targets.size(), equalTo( 6 ) );
        assertThat( targets.contains( parent ), equalTo( true ) );
        assertThat( targets.contains( papi.getTarget() ), equalTo( true ) );
        assertThat( targets.contains( art.getTarget() ), equalTo( true ) );
        assertThat( targets.contains( jarp.getTarget() ), equalTo( true ) );
        assertThat( targets.contains( comp.getTarget() ), equalTo( true ) );
        assertThat( targets.contains( wag.getTarget() ), equalTo( true ) );
    }

    @Test
    public void storeProjectAndRetrieveDependentProjectForEach()
        throws Exception
    {
        //        graph.reindex();
        //
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );

        final EProjectDirectRelationships.Builder prb = new EProjectDirectRelationships.Builder( sourceUri, p );

        final ProjectVersionRef parent = new SimpleProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        int idx = 0;
        int pidx = 0;
        final DependencyRelationship papi =
            new DependencyRelationship( sourceUri, p, new SimpleArtifactRef( "org.apache.maven", "maven-plugin-api", "3.0.3",
                                                                       null, null, false ), DependencyScope.compile,
                                        idx++, false );
        final DependencyRelationship art =
            new DependencyRelationship( sourceUri, p, new SimpleArtifactRef( "org.apache.maven", "maven-artifact", "3.0.3",
                                                                       null, null, false ), DependencyScope.compile,
                                        idx++, false );
        final PluginRelationship jarp =
            new PluginRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.plugins",
                                                                         "maven-jar-plugin", "2.2" ), pidx++, false );
        final PluginRelationship comp =
            new PluginRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.plugins",
                                                                         "maven-compiler-plugin", "2.3.2" ), pidx++,
                                    false );
        final ExtensionRelationship wag =
            new ExtensionRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.wagon",
                                                                            "wagon-provider-webdav", "1.0" ), 0 );

        prb.withParent( parent );
        prb.withDependencies( papi );
        prb.withDependencies( art );
        prb.withPlugins( jarp );
        prb.withPlugins( comp );
        prb.withExtensions( wag );

        final EProjectDirectRelationships rels = prb.build();

        final Set<ProjectRelationship<?>> rejected = graph.storeRelationships( rels.getExactAllRelationships() );
        System.out.println( "Rejects: " + rejected );

        final Map<ProjectVersionRef, Set<ProjectRelationship<?>>> byTarget =
            new HashMap<ProjectVersionRef, Set<ProjectRelationship<?>>>()
            {
                private static final long serialVersionUID = 1L;

                {
                    put( parent, graph.findDirectRelationshipsTo( parent, true, RelationshipType.values() ) );
                    put( papi.getTarget(),
                         graph.findDirectRelationshipsTo( papi.getTarget(), true, RelationshipType.values() ) );
                    put( art.getTarget(),
                         graph.findDirectRelationshipsTo( art.getTarget(), true, RelationshipType.values() ) );
                    put( jarp.getTarget(),
                         graph.findDirectRelationshipsTo( jarp.getTarget(), true, RelationshipType.values() ) );
                    put( comp.getTarget(),
                         graph.findDirectRelationshipsTo( comp.getTarget(), true, RelationshipType.values() ) );
                    put( wag.getTarget(),
                         graph.findDirectRelationshipsTo( wag.getTarget(), true, RelationshipType.values() ) );
                }
            };

        for ( final Map.Entry<ProjectVersionRef, Set<ProjectRelationship<?>>> entry : byTarget.entrySet() )
        {
            System.out.printf( "\n\n\nFor key: %s, dependencies:\n  %s\n\n\n", entry.getKey(),
                               formatWithClassname( entry.getValue(), "\n  " ) );

            assertThat( "Null dependents set for: " + entry.getKey() + "!", entry.getValue(), notNullValue() );

            assertThat( "Should have exactly one matching dependent of: " + entry.getKey(), entry.getValue()
                                                                                                 .size(), equalTo( 1 ) );
            assertThat( "Invalid dependent of: " + entry.getKey(), entry.getValue()
                                                                        .iterator()
                                                                        .next()
                                                                        .getDeclaring(), equalTo( rels.getProjectRef() ) );
        }

    }

    private String formatWithClassname( final Set<?> values, final String separator )
    {
        if ( values == null )
        {
            return "NULL";
        }

        final StringBuilder builder = new StringBuilder();
        for ( final Object value : values )
        {
            if ( builder.length() > 0 )
            {
                builder.append( separator );
            }
            builder.append( value )
                   .append( " (class: " )
                   .append( value.getClass()
                                 .getName() )
                   .append( ")" );
        }

        return builder.toString();
    }

    //    @Test
    //    public void storeProjectWithOneDependencyAndRetrieve()
    //        throws Exception
    //    {
    //        final EProjectRelationships rels =
    //            new EProjectRelationships.Builder( sourceUri, new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" ) ).withDependency( new ProjectVersionRef(
    //                                                                                                                                                           "org.apache.maven",
    //                                                                                                                                                           "maven-artifact",
    //                                                                                                                                                           "3.0.3" ),
    //                                                                                                                                    null,
    //                                                                                                                                    null,
    //                                                                                                                                    null,
    //                                                                                                                                    false )
    //                                                                                                                   .build();
    //
    //        graph.storeRelationships( rels.getExactAllRelationships() );
    //
    //        final EProjectRelationships result = graph.getProjectRelationships( rels.getProjectRef() );
    //
    //        assertThat( result.getDependencies()
    //                          .size(), equalTo( 1 ) );
    //        assertThat( result.getDependencies()
    //                          .get( 0 ), equalTo( rels.getDependencies()
    //                                                  .get( 0 ) ) );
    //    }

    @Test
    public void storeProjectWithOneDependencyAndRetrieveAsDependent()
        throws Exception
    {
        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, new SimpleProjectVersionRef( "org.apache.maven",
                                                                                       "maven-core", "3.0.3" ) ).withDependency( new SimpleProjectVersionRef(
                                                                                                                                                        "org.apache.maven",
                                                                                                                                                        "maven-artifact",
                                                                                                                                                        "3.0.3" ),
                                                                                                                                 null,
                                                                                                                                 null,
                                                                                                                                 null,
                                                                                                                                 false )
                                                                                                                .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> dependents =
            graph.findDirectRelationshipsTo( rels.getDependencies()
                                                 .get( 0 )
                                                 .getTarget(), false, RelationshipType.DEPENDENCY );

        assertThat( dependents, notNullValue() );
        assertThat( dependents.size(), equalTo( 1 ) );
        assertThat( dependents.iterator()
                              .next()
                              .getDeclaring(), equalTo( rels.getProjectRef() ) );
    }

    @Test
    public void storeProjectWithOneDependencyAndVerifyDependencyRelationship()
        throws Exception
    {
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef d = new SimpleProjectVersionRef( "org.apache.maven", "maven-artifact", "3.0.3" );

        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, p ).withDependency( d, null, null, null, false )
                                                                   .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> storedRels =
            graph.findDirectRelationshipsFrom( p, false, RelationshipType.DEPENDENCY );

        assertThat( storedRels.size(), equalTo( 1 ) );

        final ArtifactRef target = (ArtifactRef) storedRels.iterator()
                                                           .next()
                                                           .getTarget();

        assertThat( target.asProjectVersionRef(), equalTo( d ) );
    }

    @Test
    public void storeProjectWithOnePluginAndVerifyUsageRelationship()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef plugin =
            new SimpleProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.3.2" );

        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, project ).withPlugin( plugin, false )
                                                                         .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> storedRels =
            graph.findDirectRelationshipsFrom( project, false, RelationshipType.PLUGIN );

        assertThat( storedRels.size(), equalTo( 1 ) );

        final ProjectVersionRef target = storedRels.iterator()
                                                   .next()
                                                   .getTarget();

        assertThat( target, equalTo( plugin ) );
    }

    @Test
    public void storeProjectWithOnePluginAndVerifyAnyUsageRelationship()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef plugin =
            new SimpleProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.3.2" );

        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, project ).withPlugin( plugin, false )
                                                                         .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> storedRels =
            graph.findDirectRelationshipsTo( plugin, false, RelationshipType.PLUGIN );

        assertThat( storedRels.size(), equalTo( 1 ) );

        final ProjectVersionRef declaring = storedRels.iterator()
                                                      .next()
                                                      .getDeclaring();

        assertThat( declaring, equalTo( project ) );
    }

    @Test
    public void storeProjectWithOneExtensionAndVerifyUsageRelationship()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef ext = new SimpleProjectVersionRef( "org.apache.maven.wagon", "wagon-provider-webdav", "2.0" );

        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, project ).withExtension( ext )
                                                                         .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> exts =
            graph.findDirectRelationshipsFrom( project, false, RelationshipType.EXTENSION );
        assertThat( exts.size(), equalTo( 1 ) );
        assertThat( exts.iterator()
                        .next()
                        .getTarget(), equalTo( ext ) );
    }

    @Test
    public void storeProjectWithOneExtensionAndRetrieveAsUser()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef ext = new SimpleProjectVersionRef( "org.apache.maven.wagon", "wagon-provider-webdav", "2.0" );

        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, project ).withExtension( ext )
                                                                         .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> exts =
            graph.findDirectRelationshipsTo( ext, false, RelationshipType.EXTENSION );

        assertThat( exts.size(), equalTo( 1 ) );
        assertThat( exts.iterator()
                        .next()
                        .getDeclaring(), equalTo( project ) );
    }

    @Test
    public void storeProjectWithParentAndVerifyParentRelationship()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef parent = new SimpleProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, project ).withParent( parent )
                                                                         .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final ProjectVersionRef parentResult = CartoGraphUtils.getParent( project, graph );
        assertThat( parentResult, equalTo( parent ) );
    }

    @Test
    public void storeProjectWithParentAndRetrieveAsChild()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef parent = new SimpleProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        final EProjectDirectRelationships rels =
            new EProjectDirectRelationships.Builder( sourceUri, project ).withParent( parent )
                                                                         .build();

        graph.storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectVersionRef> children = CartoGraphUtils.getKnownChildren( parent, graph );

        assertThat( children.size(), equalTo( 1 ) );
        assertThat( children.iterator()
                            .next(), equalTo( project ) );
    }

}
