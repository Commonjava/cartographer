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
package org.commonjava.cartographer.testutil;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.BOM;
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
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;

public final class PresetAssertions
{

    private PresetAssertions()
    {
    }

    public static void assertConcreteAcceptance( final ProjectRelationshipFilter filter, final URI from,
                                                 final ProjectVersionRef src, final ArtifactRef tgt,
                                                 final Set<DependencyScope> acceptedScopes,
                                                 final RelationshipType... acceptances )
    {
        final Set<RelationshipType> accepted = new HashSet<RelationshipType>( Arrays.asList( acceptances ) );

        // Initially, it should accept any relationship (because they should all be necessary to build the current project)
        final ParentRelationship parent = new SimpleParentRelationship( from, src, tgt.asProjectVersionRef() );
        assertThat( "Parent acceptance does not match expectations", filter.accept( parent ),
                    equalTo( accepted.contains( PARENT ) ) );

        final PluginRelationship plugin = new SimplePluginRelationship( from, src, tgt.asProjectVersionRef(), 0, false );
        assertThat( "Plugin acceptance does not match expectations", filter.accept( plugin ),
                    equalTo( accepted.contains( PLUGIN ) ) );

        final PluginDependencyRelationship pdep =
            new SimplePluginDependencyRelationship( from, src, new SimpleProjectRef( "plugin.group", "plugin-artifact" ), tgt, 0,
                                              false );
        assertThat( "Plugin-dependency acceptance does not match expectations", filter.accept( pdep ),
                    equalTo( accepted.contains( PLUGIN_DEP ) ) );

        final ExtensionRelationship ext = new SimpleExtensionRelationship( from, src, tgt.asProjectVersionRef(), 0 );
        assertThat( "Extension acceptance does not match expectations", filter.accept( ext ),
                    equalTo( accepted.contains( EXTENSION ) ) );

        final DependencyRelationship runtimeDep = new SimpleDependencyRelationship( from, src, tgt, runtime, 0, false );
        assertThat( "Runtime dependency acceptance does not match expectations", filter.accept( runtimeDep ),
                    equalTo( accepted.contains( DEPENDENCY ) && acceptedScopes.contains( runtime ) ) );

        final DependencyRelationship testDep = new SimpleDependencyRelationship( from, src, tgt, test, 0, false );
        assertThat( "Test dependency acceptance does not match expectations", filter.accept( testDep ),
                    equalTo( accepted.contains( DEPENDENCY ) && acceptedScopes.contains( test ) ) );

        final DependencyRelationship compileDep = new SimpleDependencyRelationship( from, src, tgt, compile, 0, false );
        assertThat( "Compile dependency acceptance does not match expectations", filter.accept( compileDep ),
                    equalTo( accepted.contains( DEPENDENCY ) && acceptedScopes.contains( compile ) ) );

        final DependencyRelationship providedDep = new SimpleDependencyRelationship( from, src, tgt, provided, 0, false );
        assertThat( "Provided dependency acceptance does not match expectations", filter.accept( providedDep ),
                    equalTo( accepted.contains( DEPENDENCY ) && acceptedScopes.contains( provided ) ) );

        final DependencyRelationship embeddedDep = new SimpleDependencyRelationship( from, src, tgt, embedded, 0, false );
        //        final boolean emAccept = filter.accept( embeddedDep );
        //        final boolean emScope = acceptedScopes.contains( embedded );
        assertThat( "Embedded dependency acceptance does not match expectations", filter.accept( embeddedDep ),
                    equalTo( accepted.contains( DEPENDENCY ) && acceptedScopes.contains( embedded ) ) );

        final BomRelationship bom = new SimpleBomRelationship( from, src, tgt, 0 );
        assertThat( "BOM Dependency rejected!", filter.accept( bom ), equalTo( accepted.contains( BOM ) ) );
    }

    public static void assertRejectsAllManaged( final ProjectRelationshipFilter filter, final URI from,
                                                final ProjectVersionRef src, final ArtifactRef tgt )
    {
        // It won't accept managed relationships, though.
        final PluginRelationship managedPlugin = new SimplePluginRelationship( from, src, tgt.asProjectVersionRef(), 0, true );
        assertThat( "Managed Plugin not rejected", filter.accept( managedPlugin ), equalTo( false ) );

        final PluginDependencyRelationship managedPdep =
            new SimplePluginDependencyRelationship( from, src, new SimpleProjectRef( "plugin.group", "plugin-artifact" ), tgt, 0,
                                              true );
        assertThat( "Managed Plugin-dependency not rejected", filter.accept( managedPdep ), equalTo( false ) );

        final DependencyRelationship runtimeManagedDep =
            new SimpleDependencyRelationship( from, src, tgt, DependencyScope.runtime, 0, true );
        assertThat( "Managed Dependency not rejected", filter.accept( runtimeManagedDep ), equalTo( false ) );
    }
}
