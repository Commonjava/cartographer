/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.maven.cartographer.util;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships.Builder;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.view.DependencyView;
import org.commonjava.maven.galley.maven.view.ExtensionView;
import org.commonjava.maven.galley.maven.view.MavenPomView;
import org.commonjava.maven.galley.maven.view.ParentView;
import org.commonjava.maven.galley.maven.view.PluginView;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class MavenModelProcessor
{

    private final Logger logger = new Logger( MavenModelProcessor.class );

    @Inject
    private CartoDataManager dataManager;

    private final boolean processManagedInfo = false;

    private final boolean processBuildInfo = false;

    protected MavenModelProcessor()
    {
    }

    public MavenModelProcessor( final CartoDataManager dataManager )
    {
        this.dataManager = dataManager;
    }

    public DiscoveryResult storeModelRelationships( final MavenPomView pomView, final URI source )
        throws CartoDataException
    {
        final DiscoveryResult fromRead = readRelationships( pomView, source );
        final ProjectVersionRef projectRef = fromRead.getSelectedRef();
        dataManager.clearErrors( projectRef );
        final Set<ProjectRelationship<?>> skipped = dataManager.storeRelationships( fromRead.getAllDiscoveredRelationships() );

        return new DiscoveryResult( source, fromRead, skipped );

    }

    public DiscoveryResult readRelationships( final MavenPomView pomView, final URI source )
        throws CartoDataException
    {
        logger.info( "Reading relationships for: %s\n  (from: %s)", pomView.getRef(), source );

        try
        {
            final ProjectVersionRef projectRef = pomView.asProjectVersionRef();

            final EProjectDirectRelationships.Builder builder = new EProjectDirectRelationships.Builder( source, projectRef );

            addParentRelationship( source, builder, pomView, projectRef );

            addDependencyRelationships( source, builder, pomView, projectRef );

            if ( processBuildInfo )
            {
                addExtensionUsages( source, builder, pomView, projectRef );
                addPluginUsages( source, builder, pomView, projectRef );
            }

            final EProjectDirectRelationships rels = builder.build();
            return new DiscoveryResult( source, projectRef, rels.getAllRelationships() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new CartoDataException( "Failed to parse version string: '%s' for model: %s. Reason: %s", e, pomView.getVersion(), pomView,
                                          e.getMessage() );
        }
        catch ( final IllegalArgumentException e )
        {
            throw new CartoDataException( "Failed to parse relationships for model: %s. Reason: %s", e, pomView.getVersion(), pomView, e.getMessage() );
        }
    }

    private void addExtensionUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        final List<ExtensionView> extensions = pomView.getBuildExtensions();

        for ( final ExtensionView ext : extensions )
        {
            if ( ext == null )
            {
                continue;
            }

            if ( !ext.isValid() )
            {
                logger.warn( "Skipping invalid build extension: %s", ext.asProjectVersionRef() );
                continue;
            }

            final ProjectVersionRef ref = ext.asProjectVersionRef();

            builder.withExtensions( new ExtensionRelationship( source, projectRef, ref, builder.getNextExtensionIndex() ) );
        }
    }

    private void addPluginUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        addBuildPluginUsages( source, builder, pomView, projectRef );
        addReportPluginUsages( source, builder, pomView, projectRef );
        addSiteReportPluginUsages( source, builder, pomView, projectRef );
    }

    private void addSiteReportPluginUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        //        final List<ProjectVersionRefView> refs = pomView.getProjectVersionRefs( "//plugin[artifactId/text()=\"maven-site-plugin\"]//reportPlugin" );

        final List<PluginView> plugins = pomView.getAllPluginsMatching( "//plugin[artifactId/text()=\"maven-site-plugin\"]//reportPlugin" );
        if ( plugins != null )
        {
            for ( final PluginView plugin : plugins )
            {
                if ( !plugin.isValid() )
                {
                    logger.warn( "Skipping invalid site reporting plugin: %s", plugin.asProjectVersionRef() );
                    continue;
                }

                final ProjectVersionRef ref = plugin.asProjectVersionRef();
                final String profileId = plugin.getProfileId();
                final URI location = RelationshipUtils.profileLocation( profileId );

                builder.withPlugins( new PluginRelationship( source, location, projectRef, ref, builder.getNextPluginIndex( false ), false ) );
            }
        }
    }

    public void addReportPluginUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        final List<PluginView> plugins = pomView.getAllPluginsMatching( "//reporting/plugins/plugin" );
        if ( plugins != null )
        {
            for ( final PluginView refView : plugins )
            {
                if ( !refView.isValid() )
                {
                    logger.warn( "Skipping invalid old-style reporting plugin: %s", refView.asProjectVersionRef() );
                    continue;
                }

                final ProjectVersionRef ref = refView.asProjectVersionRef();
                final String profileId = refView.getProfileId();
                final URI location = RelationshipUtils.profileLocation( profileId );

                builder.withPlugins( new PluginRelationship( source, location, projectRef, ref, builder.getNextPluginDependencyIndex( projectRef,
                                                                                                                                      false ), false ) );
            }
        }
    }

    public void addBuildPluginUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        if ( processManagedInfo )
        {
            final List<PluginView> plugins = pomView.getAllManagedBuildPlugins();
            if ( plugins != null )
            {
                for ( final PluginView plugin : plugins )
                {
                    if ( !plugin.isValid() )
                    {
                        logger.warn( "Skipping invalid managed plugin: %s", plugin.asProjectVersionRef() );
                        continue;
                    }

                    final ProjectVersionRef ref = plugin.asProjectVersionRef();

                    final String profileId = plugin.getProfileId();
                    final URI location = RelationshipUtils.profileLocation( profileId );

                    builder.withPlugins( new PluginRelationship( source, location, projectRef, ref, builder.getNextPluginDependencyIndex( projectRef,
                                                                                                                                          true ),
                                                                 true ) );
                }
            }
        }

        final List<PluginView> plugins = pomView.getAllBuildPlugins();
        if ( plugins != null )
        {
            for ( final PluginView plugin : plugins )
            {
                if ( !plugin.isValid() )
                {
                    logger.warn( "Skipping invalid plugin: %s", plugin.asProjectVersionRef() );
                    continue;
                }

                final ProjectVersionRef ref = plugin.asProjectVersionRef();
                final String profileId = plugin.getProfileId();
                final URI location = RelationshipUtils.profileLocation( profileId );

                builder.withPlugins( new PluginRelationship( source, location, projectRef, ref, builder.getNextPluginDependencyIndex( projectRef,
                                                                                                                                      false ), false ) );
            }
        }
    }

    public void addDependencyRelationships( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException, InvalidVersionSpecificationException
    {
        if ( processManagedInfo )
        {
            final List<DependencyView> deps = pomView.getAllManagedDependencies();
            if ( deps != null )
            {
                for ( final DependencyView dep : deps )
                {
                    if ( !dep.isValid() )
                    {
                        logger.warn( "Skipping invalid managed dependency: %s", dep.asArtifactRef() );
                        continue;
                    }

                    final ProjectVersionRef ref = dep.asProjectVersionRef();

                    final String profileId = dep.getProfileId();
                    final URI location = RelationshipUtils.profileLocation( profileId );

                    final ArtifactRef artifactRef = new ArtifactRef( ref, dep.getType(), dep.getClassifier(), dep.isOptional() );

                    builder.withDependencies( new DependencyRelationship( source, location, projectRef, artifactRef, dep.getScope(),
                                                                          builder.getNextDependencyIndex( true ), true ) );
                }
            }
        }

        // regardless of whether we're processing managed info, this is STRUCTURAL, so always grab it!
        final List<DependencyView> boms = pomView.getAllBOMs();
        if ( boms != null )
        {
            for ( final DependencyView bom : boms )
            {
                if ( !bom.isValid() )
                {
                    logger.warn( "Skipping invalid dependency: %s", bom.asArtifactRef() );
                    continue;
                }

                final ProjectVersionRef ref = bom.asProjectVersionRef();
                final String profileId = bom.getProfileId();
                final URI location = RelationshipUtils.profileLocation( profileId );

                final ArtifactRef artifactRef = new ArtifactRef( ref, bom.getType(), bom.getClassifier(), bom.isOptional() );

                builder.withDependencies( new DependencyRelationship( source, location, projectRef, artifactRef, bom.getScope(),
                                                                      builder.getNextDependencyIndex( true ), true ) );
            }
        }

        final List<DependencyView> deps = pomView.getAllDirectDependencies();
        if ( deps != null )
        {
            for ( final DependencyView dep : deps )
            {
                if ( !dep.isValid() )
                {
                    logger.warn( "Skipping invalid dependency: %s", dep.asArtifactRef() );
                    continue;
                }

                final ProjectVersionRef ref = dep.asProjectVersionRef();
                final String profileId = dep.getProfileId();
                final URI location = RelationshipUtils.profileLocation( profileId );

                final ArtifactRef artifactRef = new ArtifactRef( ref, dep.getType(), dep.getClassifier(), dep.isOptional() );

                builder.withDependencies( new DependencyRelationship( source, location, projectRef, artifactRef, dep.getScope(),
                                                                      builder.getNextDependencyIndex( false ), false ) );
            }
        }
    }

    public void addParentRelationship( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        final ParentView parent = pomView.getParent();
        if ( parent != null )
        {
            if ( !parent.isValid() )
            {
                logger.warn( "Skipping invalid parent declaration: %s", parent.asProjectVersionRef() );
                return;
            }

            final ProjectVersionRef ref = parent.asProjectVersionRef();
            builder.withParent( new ParentRelationship( source, builder.getProjectRef(), ref ) );
        }
        else
        {
            builder.withParent( new ParentRelationship( source, builder.getProjectRef() ) );
        }
    }

}
