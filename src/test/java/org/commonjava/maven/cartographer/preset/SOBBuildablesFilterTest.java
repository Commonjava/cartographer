package org.commonjava.maven.cartographer.preset;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;
import static org.commonjava.maven.atlas.ident.DependencyScope.compile;
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

    private SOBBuildablesFilter filter;

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
        filter = new SOBBuildablesFilter();
        from = new URI( "test:source" );
        root = new ProjectVersionRef( "group", "root", "1" );
        src = new ProjectVersionRef( "group.id", "artifact-id", "1.0" );
        tgt = new ArtifactRef( "other.group", "other-artifact", "2.0", "jar", null, false );
    }

    @Test
    public void initialInstanceAcceptsRuntimeAndCompileDependencies_AndParent()
        throws Exception
    {
        assertConcreteAcceptance( filter, from, src, tgt, new HashSet<>( Arrays.asList( runtime, compile ) ), DEPENDENCY, PARENT );
    }

    @Test
    public void initialInstanceRejectsAllManagedRelationships()
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
    public void acceptAllRuntimeAndCompileDependenciesWithParentsAfterTraversingRuntimeDependency()
        throws Exception
    {
        final DependencyRelationship dep = new DependencyRelationship( from, root, new ArtifactRef( src, "jar", null, false ), runtime, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>( Arrays.asList( runtime, compile ) ), DEPENDENCY, PARENT );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllRuntimeAndCompileDependenciesWithParentsAfterTraversingCompileDependency()
        throws Exception
    {
        final DependencyRelationship dep = new DependencyRelationship( from, root, new ArtifactRef( src, "jar", null, false ), compile, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>( Arrays.asList( runtime, compile ) ), DEPENDENCY, PARENT );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllRuntimeAndCompileDependenciesWithParentsAfterTraversingParent()
        throws Exception
    {
        final ParentRelationship parent = new ParentRelationship( from, src, root );

        final ProjectRelationshipFilter child = filter.getChildFilter( parent );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>( Arrays.asList( runtime, compile ) ), DEPENDENCY, PARENT );

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
