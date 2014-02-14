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
package org.commonjava.maven.cartographer.preset;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;
import static org.commonjava.maven.atlas.ident.DependencyScope.compile;
import static org.commonjava.maven.atlas.ident.DependencyScope.embedded;
import static org.commonjava.maven.atlas.ident.DependencyScope.provided;
import static org.commonjava.maven.atlas.ident.DependencyScope.runtime;
import static org.commonjava.maven.atlas.ident.DependencyScope.test;
import static org.commonjava.maven.cartographer.testutil.PresetAssertions.assertConcreteAcceptance;
import static org.commonjava.maven.cartographer.testutil.PresetAssertions.assertRejectsAllManaged;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SOBBuildablesFilterTest
{

    private ScopeWithEmbeddedProjectsFilter filter;

    private URI from;

    private ProjectVersionRef src;

    private ProjectVersionRef root;

    private ArtifactRef tgt;

    @BeforeClass
    public static void setupLogging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Before
    public void setup()
        throws Exception
    {
        filter = new ScopeWithEmbeddedProjectsFilter( DependencyScope.runtime, false );
        from = new URI( "test:source" );
        root = new ProjectVersionRef( "group", "root", "1" );
        src = new ProjectVersionRef( "group.id", "artifact-id", "1.0" );
        tgt = new ArtifactRef( "other.group", "other-artifact", "2.0", "jar", null, false );
    }

    @Test
    public void initialInstanceAcceptsRuntimeAndCompileDependencies_AndParent()
        throws Exception
    {
        assertConcreteAcceptance( filter, from, src, tgt, new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ), DEPENDENCY,
                                  PARENT );
    }

    @Test
    public void initialInstanceRejectsAllManagedRelationships_EXCEPT_BOM()
    {
        assertRejectsAllManaged( filter, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterTraversingPlugin()
        throws Exception
    {
        final PluginRelationship plugin = new PluginRelationship( from, root, src, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( plugin );
        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>() );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterTraversingExtension()
        throws Exception
    {
        final ExtensionRelationship plugin = new ExtensionRelationship( from, root, src, 0 );

        final ProjectRelationshipFilter child = filter.getChildFilter( plugin );
        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>() );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterTestDependency()
        throws Exception
    {
        final DependencyRelationship dep = new DependencyRelationship( from, root, new ArtifactRef( src, "jar", null, false ), test, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>() );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterProvidedDependency()
        throws Exception
    {
        final DependencyRelationship dep = new DependencyRelationship( from, root, new ArtifactRef( src, "jar", null, false ), provided, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>() );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllEmbeddedAndRuntimeAndCompileDependenciesWithParentsAfterTraversingRuntimeDependency()
        throws Exception
    {
        final DependencyRelationship dep = new DependencyRelationship( from, root, new ArtifactRef( src, "jar", null, false ), runtime, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ), DEPENDENCY,
                                  PARENT );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllEmbeddedAndRuntimeAndCompileDependenciesWithParentsAfterTraversingCompileDependency()
        throws Exception
    {
        final DependencyRelationship dep = new DependencyRelationship( from, root, new ArtifactRef( src, "jar", null, false ), compile, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ), DEPENDENCY,
                                  PARENT );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllEmbeddedAndRuntimeAndCompileDependenciesWithParentsAfterTraversingParent()
        throws Exception
    {
        final ParentRelationship parent = new ParentRelationship( from, src, root );

        final ProjectRelationshipFilter child = filter.getChildFilter( parent );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ), DEPENDENCY,
                                  PARENT );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptParentAfterTraversingParent()
        throws Exception
    {
        final ProjectVersionRef parentRef = new ProjectVersionRef( "group.id", "intermediate-parent", "2" );
        final ParentRelationship parent = new ParentRelationship( from, src, parentRef );

        final ProjectRelationshipFilter child = filter.getChildFilter( parent );

        final ParentRelationship gparent = new ParentRelationship( from, parentRef, root );
        assertThat( child.accept( gparent ), equalTo( true ) );
    }

}
