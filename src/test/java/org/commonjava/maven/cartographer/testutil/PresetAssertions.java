package org.commonjava.maven.cartographer.testutil;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.EXTENSION;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PLUGIN;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PLUGIN_DEP;
import static org.commonjava.maven.atlas.ident.DependencyScope.compile;
import static org.commonjava.maven.atlas.ident.DependencyScope.embedded;
import static org.commonjava.maven.atlas.ident.DependencyScope.provided;
import static org.commonjava.maven.atlas.ident.DependencyScope.runtime;
import static org.commonjava.maven.atlas.ident.DependencyScope.test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class PresetAssertions
{

    private PresetAssertions()
    {
    }

    public static void assertConcreteAcceptance( final ProjectRelationshipFilter filter, final URI from, final ProjectVersionRef src,
                                                 final ArtifactRef tgt, final Set<DependencyScope> acceptedScopes,
                                                 final RelationshipType... acceptances )
    {
        final Set<RelationshipType> accepted = new HashSet<RelationshipType>( Arrays.asList( acceptances ) );

        // Initially, it should accept any relationship (because they should all be necessary to build the current project)
        final ParentRelationship parent = new ParentRelationship( from, src, tgt.asProjectVersionRef() );
        assertThat( "Parent acceptance does not match expectations", filter.accept( parent ), equalTo( accepted.contains( PARENT ) ) );

        final PluginRelationship plugin = new PluginRelationship( from, src, tgt.asProjectVersionRef(), 0, false );
        assertThat( "Plugin acceptance does not match expectations", filter.accept( plugin ), equalTo( accepted.contains( PLUGIN ) ) );

        final PluginDependencyRelationship pdep =
            new PluginDependencyRelationship( from, src, new ProjectRef( "plugin.group", "plugin-artifact" ), tgt, 0, false );
        assertThat( "Plugin-dependency acceptance does not match expectations", filter.accept( pdep ), equalTo( accepted.contains( PLUGIN_DEP ) ) );

        final ExtensionRelationship ext = new ExtensionRelationship( from, src, tgt.asProjectVersionRef(), 0 );
        assertThat( "Extension acceptance does not match expectations", filter.accept( ext ), equalTo( accepted.contains( EXTENSION ) ) );

        final DependencyRelationship runtimeDep = new DependencyRelationship( from, src, tgt, runtime, 0, false );
        assertThat( "Runtime dependency acceptance does not match expectations", filter.accept( runtimeDep ), equalTo( accepted.contains( DEPENDENCY )
            && acceptedScopes.contains( runtime ) ) );

        final DependencyRelationship testDep = new DependencyRelationship( from, src, tgt, test, 0, false );
        assertThat( "Test dependency acceptance does not match expectations", filter.accept( testDep ), equalTo( accepted.contains( DEPENDENCY )
            && acceptedScopes.contains( test ) ) );

        final DependencyRelationship compileDep = new DependencyRelationship( from, src, tgt, compile, 0, false );
        assertThat( "Compile dependency acceptance does not match expectations", filter.accept( compileDep ), equalTo( accepted.contains( DEPENDENCY )
            && acceptedScopes.contains( compile ) ) );

        final DependencyRelationship providedDep = new DependencyRelationship( from, src, tgt, provided, 0, false );
        assertThat( "Provided dependency acceptance does not match expectations", filter.accept( providedDep ),
                    equalTo( accepted.contains( DEPENDENCY ) && acceptedScopes.contains( provided ) ) );

        final DependencyRelationship embeddedDep = new DependencyRelationship( from, src, tgt, embedded, 0, false );
        assertThat( "Provided dependency acceptance does not match expectations", filter.accept( embeddedDep ),
                    equalTo( accepted.contains( DEPENDENCY ) && acceptedScopes.contains( embedded ) ) );
    }

    public static void assertRejectsAllManaged( final ProjectRelationshipFilter filter, final URI from, final ProjectVersionRef src,
                                                final ArtifactRef tgt )
    {
        // It won't accept managed relationships, though.
        final PluginRelationship managedPlugin = new PluginRelationship( from, src, tgt.asProjectVersionRef(), 0, true );
        assertThat( "Managed Plugin not rejected", filter.accept( managedPlugin ), equalTo( false ) );

        final PluginDependencyRelationship managedPdep =
            new PluginDependencyRelationship( from, src, new ProjectRef( "plugin.group", "plugin-artifact" ), tgt, 0, true );
        assertThat( "Managed Plugin-dependency not rejected", filter.accept( managedPdep ), equalTo( false ) );

        final DependencyRelationship runtimeManagedDep = new DependencyRelationship( from, src, tgt, DependencyScope.runtime, 0, true );
        assertThat( "Managed Dependency not rejected", filter.accept( runtimeManagedDep ), equalTo( false ) );

        final DependencyRelationship bom = new DependencyRelationship( from, src, tgt.asPomArtifact(), DependencyScope._import, 0, true );
        assertThat( "BOM Dependency rejected!", filter.accept( bom ), equalTo( true ) );
    }
}
