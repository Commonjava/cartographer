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
package org.commonjava.maven.cartographer.data;

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

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.commonjava.maven.atlas.graph.filter.DependencyOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.ExtensionOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.PluginOnlyFilter;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class AbstractCartoDataManagerTest
{

    @BeforeClass
    public static void initLogging()
    {
        LogManager.getLoggerRepository()
                  .setThreshold( Level.DEBUG );

        //        LogManager.getRootLogger()
        //                  .addAppender( new ConsoleAppender( new SimpleLayout() ) );
        LogManager.getRootLogger()
                  .addAppender( new ConsoleAppender( new PatternLayout() ) );

        LogManager.getRootLogger()
                  .setLevel( Level.DEBUG );
    }

    protected abstract GraphWorkspaceHolder getSessionManager()
        throws Exception;

    protected abstract CartoDataManager getDataManager()
        throws Exception;

    protected abstract GraphAggregator getAggregator()
        throws Exception;

    protected abstract void setupComponents()
        throws Exception;

    private URI sourceUri;

    @Before
    public void setupSession()
        throws Exception
    {
        setupComponents();
        getDataManager().createWorkspace( sourceUri() );
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
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final EProjectDirectRelationships root = new EProjectDirectRelationships.Builder( sourceUri, r ).build();

        getDataManager().storeRelationships( root.getExactAllRelationships() );

        assertThat( getDataManager().contains( r ), equalTo( true ) );
    }

    @Test
    public void storeParentChildDescendantProjectsAndRetrieveAncestryViaGraphOfChild()
        throws Exception
    {
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "org.test", "child", "1.0" );

        final EProjectDirectRelationships root = new EProjectDirectRelationships.Builder( sourceUri, r ).build();
        final EProjectDirectRelationships parent = new EProjectDirectRelationships.Builder( sourceUri, p ).withParent( r )
                                                                                                          .build();
        final EProjectDirectRelationships child = new EProjectDirectRelationships.Builder( sourceUri, c ).withParent( p )
                                                                                                         .build();

        getDataManager().storeRelationships( root.getExactAllRelationships() );
        getDataManager().storeRelationships( parent.getExactAllRelationships() );
        getDataManager().storeRelationships( child.getExactAllRelationships() );

        final EProjectGraph graph = getDataManager().getProjectGraph( c );

        //        final AggregationOptions aggConf = new DefaultAggregatorOptions();
        //        graph = getAggregator().connectSubgraphs( graph, aggConf, false );
        //
        final Set<ProjectVersionRef> incompleteSubgraphs = graph.getIncompleteSubgraphs();
        System.out.println( incompleteSubgraphs );
        assertThat( graph.isComplete(), equalTo( true ) );

        final AncestryTraversal ancestryTraversal = new AncestryTraversal();
        graph.traverse( ancestryTraversal );

        final List<ProjectVersionRef> ancestry = ancestryTraversal.getAncestry();
        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

    @Test
    public void storeParentChildDescendantProjectsAndRetrieveAncestry()
        throws Exception
    {
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "org.test", "child", "1.0" );

        final EProjectDirectRelationships root = new EProjectDirectRelationships.Builder( sourceUri, r ).build();
        final EProjectDirectRelationships parent = new EProjectDirectRelationships.Builder( sourceUri, p ).withParent( r )
                                                                                                          .build();
        final EProjectDirectRelationships child = new EProjectDirectRelationships.Builder( sourceUri, c ).withParent( p )
                                                                                                         .build();

        getDataManager().storeRelationships( root.getExactAllRelationships() );
        getDataManager().storeRelationships( parent.getExactAllRelationships() );
        getDataManager().storeRelationships( child.getExactAllRelationships() );

        final List<ProjectVersionRef> ancestry = getDataManager().getAncestry( c );
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
        final ProjectVersionRef p = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );

        final EProjectDirectRelationships.Builder prb = new EProjectDirectRelationships.Builder( sourceUri, p );

        final ProjectVersionRef parent = new ProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        int idx = 0;
        int pidx = 0;
        final DependencyRelationship papi =
            new DependencyRelationship( sourceUri, p, new ArtifactRef( "org.apache.maven", "maven-plugin-api", "3.0.3", null, null, false ),
                                        DependencyScope.compile, idx++, false );
        final DependencyRelationship art =
            new DependencyRelationship( sourceUri, p, new ArtifactRef( "org.apache.maven", "maven-artifact", "3.0.3", null, null, false ),
                                        DependencyScope.compile, idx++, false );
        final PluginRelationship jarp =
            new PluginRelationship( sourceUri, p, new ProjectVersionRef( "org.apache.maven.plugins", "maven-jar-plugin", "2.2" ), pidx++, false );
        final PluginRelationship comp =
            new PluginRelationship( sourceUri, p, new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.3.2" ), pidx++,
                                    false );
        final ExtensionRelationship wag =
            new ExtensionRelationship( sourceUri, p, new ProjectVersionRef( "org.apache.maven.wagon", "wagon-provider-webdav", "1.0" ), 0 );

        prb.withParent( parent );
        prb.withDependencies( papi, art );
        prb.withPlugins( jarp, comp );
        prb.withExtensions( wag );

        final EProjectDirectRelationships rels = prb.build();

        assertThat( rels.getAllRelationships()
                        .size(), equalTo( 6 ) );

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> resulting = getDataManager().getAllDirectRelationshipsWithExactSource( rels.getProjectRef(), null, null );

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
        //        getDataManager().reindex();
        //
        final ProjectVersionRef p = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );

        final EProjectDirectRelationships.Builder prb = new EProjectDirectRelationships.Builder( sourceUri, p );

        final ProjectVersionRef parent = new ProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        int idx = 0;
        int pidx = 0;
        final DependencyRelationship papi =
            new DependencyRelationship( sourceUri, p, new ArtifactRef( "org.apache.maven", "maven-plugin-api", "3.0.3", null, null, false ),
                                        DependencyScope.compile, idx++, false );
        final DependencyRelationship art =
            new DependencyRelationship( sourceUri, p, new ArtifactRef( "org.apache.maven", "maven-artifact", "3.0.3", null, null, false ),
                                        DependencyScope.compile, idx++, false );
        final PluginRelationship jarp =
            new PluginRelationship( sourceUri, p, new ProjectVersionRef( "org.apache.maven.plugins", "maven-jar-plugin", "2.2" ), pidx++, false );
        final PluginRelationship comp =
            new PluginRelationship( sourceUri, p, new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.3.2" ), pidx++,
                                    false );
        final ExtensionRelationship wag =
            new ExtensionRelationship( sourceUri, p, new ProjectVersionRef( "org.apache.maven.wagon", "wagon-provider-webdav", "1.0" ), 0 );

        prb.withParent( parent );
        prb.withDependencies( papi );
        prb.withDependencies( art );
        prb.withPlugins( jarp );
        prb.withPlugins( comp );
        prb.withExtensions( wag );

        final EProjectDirectRelationships rels = prb.build();

        final Set<ProjectRelationship<?>> rejected = getDataManager().storeRelationships( rels.getExactAllRelationships() );
        System.out.println( "Rejects: " + rejected );

        final Map<ProjectVersionRef, Set<ProjectRelationship<?>>> byTarget = new HashMap<ProjectVersionRef, Set<ProjectRelationship<?>>>()
        {
            private static final long serialVersionUID = 1L;

            {
                put( parent, getDataManager().getAllDirectRelationshipsWithExactTarget( parent, null, null ) );
                put( papi.getTarget(), getDataManager().getAllDirectRelationshipsWithExactTarget( papi.getTarget(), null, null ) );
                put( art.getTarget(), getDataManager().getAllDirectRelationshipsWithExactTarget( art.getTarget(), null, null ) );
                put( jarp.getTarget(), getDataManager().getAllDirectRelationshipsWithExactTarget( jarp.getTarget(), null, null ) );
                put( comp.getTarget(), getDataManager().getAllDirectRelationshipsWithExactTarget( comp.getTarget(), null, null ) );
                put( wag.getTarget(), getDataManager().getAllDirectRelationshipsWithExactTarget( wag.getTarget(), null, null ) );
            }
        };

        for ( final Map.Entry<ProjectVersionRef, Set<ProjectRelationship<?>>> entry : byTarget.entrySet() )
        {
            System.out.printf( "\n\n\nFor key: %s, dependencies:\n  %s\n\n\n", entry.getKey(), formatWithClassname( entry.getValue(), "\n  " ) );

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
    //        getDataManager().storeRelationships( rels.getExactAllRelationships() );
    //
    //        final EProjectRelationships result = getDataManager().getProjectRelationships( rels.getProjectRef() );
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
            new EProjectDirectRelationships.Builder( sourceUri, new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" ) ).withDependency( new ProjectVersionRef(
                                                                                                                                                                            "org.apache.maven",
                                                                                                                                                                            "maven-artifact",
                                                                                                                                                                            "3.0.3" ),
                                                                                                                                                     null,
                                                                                                                                                     null,
                                                                                                                                                     null,
                                                                                                                                                     false )
                                                                                                                                    .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> dependents = getDataManager().getAllDirectRelationshipsWithExactTarget( rels.getDependencies()
                                                                                                                      .get( 0 )
                                                                                                                      .getTarget(), null, null );

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
        final ProjectVersionRef p = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef d = new ProjectVersionRef( "org.apache.maven", "maven-artifact", "3.0.3" );

        final EProjectDirectRelationships rels = new EProjectDirectRelationships.Builder( sourceUri, p ).withDependency( d, null, null, null, false )
                                                                                                        .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> storedRels = getDataManager().getAllDirectRelationshipsWithExactSource( p, new DependencyOnlyFilter(), null );

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
        final ProjectVersionRef project = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef plugin = new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.3.2" );

        final EProjectDirectRelationships rels = new EProjectDirectRelationships.Builder( sourceUri, project ).withPlugin( plugin, false )
                                                                                                              .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> storedRels = getDataManager().getAllDirectRelationshipsWithExactSource( project, new PluginOnlyFilter(), null );

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
        final ProjectVersionRef project = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef plugin = new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.3.2" );

        final EProjectDirectRelationships rels = new EProjectDirectRelationships.Builder( sourceUri, project ).withPlugin( plugin, false )
                                                                                                              .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> storedRels = getDataManager().getAllDirectRelationshipsWithExactTarget( plugin, new PluginOnlyFilter(), null );

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
        final ProjectVersionRef project = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef ext = new ProjectVersionRef( "org.apache.maven.wagon", "wagon-provider-webdav", "2.0" );

        final EProjectDirectRelationships rels = new EProjectDirectRelationships.Builder( sourceUri, project ).withExtension( ext )
                                                                                                              .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> exts = getDataManager().getAllDirectRelationshipsWithExactSource( project, new ExtensionOnlyFilter(), null );
        assertThat( exts.size(), equalTo( 1 ) );
        assertThat( exts.iterator()
                        .next()
                        .getTarget(), equalTo( ext ) );
    }

    @Test
    public void storeProjectWithOneExtensionAndRetrieveAsUser()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef ext = new ProjectVersionRef( "org.apache.maven.wagon", "wagon-provider-webdav", "2.0" );

        final EProjectDirectRelationships rels = new EProjectDirectRelationships.Builder( sourceUri, project ).withExtension( ext )
                                                                                                              .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectRelationship<?>> exts = getDataManager().getAllDirectRelationshipsWithExactTarget( ext, new ExtensionOnlyFilter(), null );

        assertThat( exts.size(), equalTo( 1 ) );
        assertThat( exts.iterator()
                        .next()
                        .getDeclaring(), equalTo( project ) );
    }

    @Test
    public void storeProjectWithParentAndVerifyParentRelationship()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef parent = new ProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        final EProjectDirectRelationships rels = new EProjectDirectRelationships.Builder( sourceUri, project ).withParent( parent )
                                                                                                              .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final ProjectVersionRef parentResult = getDataManager().getParent( project );
        assertThat( parentResult, equalTo( parent ) );
    }

    @Test
    public void storeProjectWithParentAndRetrieveAsChild()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );
        final ProjectVersionRef parent = new ProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        final EProjectDirectRelationships rels = new EProjectDirectRelationships.Builder( sourceUri, project ).withParent( parent )
                                                                                                              .build();

        getDataManager().storeRelationships( rels.getExactAllRelationships() );

        final Set<ProjectVersionRef> children = getDataManager().getKnownChildren( parent );

        assertThat( children.size(), equalTo( 1 ) );
        assertThat( children.iterator()
                            .next(), equalTo( project ) );
    }

}
