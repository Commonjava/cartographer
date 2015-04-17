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
package org.commonjava.maven.cartographer.discover.patch;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.post.patch.DependencyPluginPatcher;
import org.commonjava.maven.galley.maven.util.ArtifactPathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.testing.core.transport.job.TestDownload;
import org.junit.Before;
import org.junit.Test;

public class DependencyPluginPatcherTest
    extends AbstractPatcherTest
{

    private static final String BASE = "patchers/dependency-plugin/";

    protected DependencyPluginPatcher patcher;

    @Before
    public void setup()
        throws Exception
    {
        setupGalley();
        patcher = new DependencyPluginPatcher();
    }

    private final URI profileLoc = RelationshipUtils.profileLocation( "test-profile" );

    @Test
    public void addDepFromDepPluginArtifactItemNotReferencingExisting()
        throws Exception
    {
        final Location location = new SimpleLocation( "test-repo", "http://www.nowhere.com/path/to/repo" );
        final ProjectVersionRef pvr = new ProjectVersionRef( "group.id", "artifact-id", "1" );

        final String pom = BASE + "no-existing-dep.pom.xml";
        final TestDownload download = new TestDownload( pom );

        final ConcreteResource resource =
            new ConcreteResource( location, ArtifactPathUtils.formatArtifactPath( pvr.asPomArtifact(),
                                                                                  galleyFixture.getMapper() ) );

        galleyFixture.getTransport()
                     .registerDownload( resource, download );

        final URI src = new URI( "test:uri" );
        final DiscoveryResult result = new DiscoveryResult( src, pvr, new HashSet<ProjectRelationship<?>>() );
        patcher.patch( result, Arrays.asList( location ), getContext( pvr, location ) );

        final Set<ProjectRelationship<?>> newRels = result.getAcceptedRelationships();

        assertThat( newRels, notNullValue() );
        assertThat( newRels.size(), equalTo( 1 ) );

        for ( final ProjectRelationship<?> rel : result.getAcceptedRelationships() )
        {
            assertThat( rel + " is not a dependency!", rel instanceof DependencyRelationship, equalTo( true ) );
            assertThat( rel + " is not of scope embedded!", ( (DependencyRelationship) rel ).getScope(),
                        equalTo( DependencyScope.embedded ) );
            assertThat( rel + " is not declared in pom root!", rel.getPomLocation(),
                        equalTo( RelationshipUtils.POM_ROOT_URI ) );
        }
    }

    @Test
    public void switchScopeOnReferencedExistingProvidedScopeDep()
        throws Exception
    {
        final Location location = new SimpleLocation( "test-repo", "http://www.nowhere.com/path/to/repo" );
        final ProjectVersionRef pvr = new ProjectVersionRef( "group.id", "artifact-id", "1" );

        final String pom = BASE + "existing-provided-dep.pom.xml";
        final TestDownload download = new TestDownload( pom );

        final ConcreteResource resource =
            new ConcreteResource( location, ArtifactPathUtils.formatArtifactPath( pvr.asPomArtifact(),
                                                                                  galleyFixture.getMapper() ) );

        galleyFixture.getTransport()
                     .registerDownload( resource, download );

        final URI src = new URI( "test:uri" );
        final DiscoveryResult result =
            new DiscoveryResult( src, pvr, parseDependencyRelationships( pom, pvr, location, src ) );
        final Set<ProjectRelationship<?>> oldRels = result.getAcceptedRelationships();

        patcher.patch( result, Arrays.asList( location ), getContext( pvr, location ) );

        final Set<ProjectRelationship<?>> newRels = result.getAcceptedRelationships();
        newRels.removeAll( oldRels );

        assertThat( newRels, notNullValue() );
        assertThat( newRels.size(), equalTo( 0 ) );

        for ( final ProjectRelationship<?> rel : result.getAcceptedRelationships() )
        {
            assertThat( rel + " is not a dependency!", rel instanceof DependencyRelationship, equalTo( true ) );
            assertThat( rel + " is not of scope embedded!", ( (DependencyRelationship) rel ).getScope(),
                        equalTo( DependencyScope.embedded ) );
            assertThat( rel + " is not declared in pom root!", rel.getPomLocation(),
                        equalTo( RelationshipUtils.POM_ROOT_URI ) );
        }
    }

    @Test
    public void profile_addDepFromDepPluginArtifactItemNotReferencingExisting()
        throws Exception
    {
        final Location location = new SimpleLocation( "test-repo", "http://www.nowhere.com/path/to/repo" );
        final ProjectVersionRef pvr = new ProjectVersionRef( "group.id", "artifact-id", "1" );

        final String pom = BASE + "in-profile-no-dep.pom.xml";
        final TestDownload download = new TestDownload( pom );

        final ConcreteResource resource =
            new ConcreteResource( location, ArtifactPathUtils.formatArtifactPath( pvr.asPomArtifact(),
                                                                                  galleyFixture.getMapper() ) );

        galleyFixture.getTransport()
                     .registerDownload( resource, download );

        final URI src = new URI( "test:uri" );
        final DiscoveryResult result = new DiscoveryResult( src, pvr, new HashSet<ProjectRelationship<?>>() );
        patcher.patch( result, Arrays.asList( location ), getContext( pvr, location ) );

        final Set<ProjectRelationship<?>> newRels = result.getAcceptedRelationships();

        assertThat( newRels, notNullValue() );
        assertThat( newRels.size(), equalTo( 1 ) );

        for ( final ProjectRelationship<?> rel : result.getAcceptedRelationships() )
        {
            assertThat( rel + " is not a dependency!", rel instanceof DependencyRelationship, equalTo( true ) );
            assertThat( rel + " is not of scope embedded!", ( (DependencyRelationship) rel ).getScope(),
                        equalTo( DependencyScope.embedded ) );
            assertThat( rel + " is not declared in the profile!", rel.getPomLocation(), equalTo( profileLoc ) );
        }
    }

    @Test
    public void profile_switchScopeOnReferencedExistingProvidedScopeDep()
        throws Exception
    {
        final Location location = new SimpleLocation( "test-repo", "http://www.nowhere.com/path/to/repo" );
        final ProjectVersionRef pvr = new ProjectVersionRef( "group.id", "artifact-id", "1" );

        final String pom = BASE + "in-profile-provided-dep.pom.xml";
        final TestDownload download = new TestDownload( pom );

        final ConcreteResource resource =
            new ConcreteResource( location, ArtifactPathUtils.formatArtifactPath( pvr.asPomArtifact(),
                                                                                  galleyFixture.getMapper() ) );

        galleyFixture.getTransport()
                     .registerDownload( resource, download );

        final URI src = new URI( "test:uri" );
        final DiscoveryResult result =
            new DiscoveryResult( src, pvr, parseDependencyRelationships( pom, pvr, location, src ) );
        final Set<ProjectRelationship<?>> oldRels = result.getAcceptedRelationships();

        patcher.patch( result, Arrays.asList( location ), getContext( pvr, location ) );

        final Set<ProjectRelationship<?>> newRels = result.getAcceptedRelationships();
        newRels.removeAll( oldRels );

        assertThat( newRels, notNullValue() );
        assertThat( newRels.size(), equalTo( 0 ) );

        for ( final ProjectRelationship<?> rel : result.getAcceptedRelationships() )
        {
            assertThat( rel + " is not a dependency!", rel instanceof DependencyRelationship, equalTo( true ) );
            assertThat( rel + " is not of scope embedded!", ( (DependencyRelationship) rel ).getScope(),
                        equalTo( DependencyScope.embedded ) );
            assertThat( rel + " is not declared in the profile!", rel.getPomLocation(), equalTo( profileLoc ) );
        }
    }

    @Test
    public void profileAndRoot_switchScopeOnReferencedExistingProvidedScopeDep()
        throws Exception
    {
        final Location location = new SimpleLocation( "test-repo", "http://www.nowhere.com/path/to/repo" );
        final ProjectVersionRef pvr = new ProjectVersionRef( "group.id", "artifact-id", "1" );

        final String pom = BASE + "in-profile-root-provided-dep.pom.xml";
        final TestDownload download = new TestDownload( pom );

        final ConcreteResource resource =
            new ConcreteResource( location, ArtifactPathUtils.formatArtifactPath( pvr.asPomArtifact(),
                                                                                  galleyFixture.getMapper() ) );

        galleyFixture.getTransport()
                     .registerDownload( resource, download );

        final URI src = new URI( "test:uri" );
        final DiscoveryResult result =
            new DiscoveryResult( src, pvr, parseDependencyRelationships( pom, pvr, location, src ) );
        final Set<ProjectRelationship<?>> oldRels = result.getAcceptedRelationships();

        patcher.patch( result, Arrays.asList( location ), getContext( pvr, location ) );

        final Set<ProjectRelationship<?>> newRels = result.getAcceptedRelationships();
        newRels.removeAll( oldRels );

        assertThat( newRels, notNullValue() );
        assertThat( newRels.size(), equalTo( 0 ) );

        for ( final ProjectRelationship<?> rel : result.getAcceptedRelationships() )
        {
            assertThat( rel + " is not a dependency!", rel instanceof DependencyRelationship, equalTo( true ) );
            assertThat( rel + " is not of scope embedded!", ( (DependencyRelationship) rel ).getScope(),
                        equalTo( DependencyScope.embedded ) );
            assertThat( rel + " is not declared in the pom root!", rel.getPomLocation(),
                        equalTo( RelationshipUtils.POM_ROOT_URI ) );
        }
    }

}
