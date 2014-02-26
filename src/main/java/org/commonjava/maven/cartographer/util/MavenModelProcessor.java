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
package org.commonjava.maven.cartographer.util;

import static org.apache.commons.lang.StringUtils.join;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships.Builder;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.ExtensionView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.ParentView;
import org.commonjava.maven.galley.maven.model.view.PluginDependencyView;
import org.commonjava.maven.galley.maven.model.view.PluginView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MavenModelProcessor
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CartoDataManager dataManager;

    private final boolean processManagedDependencyInfo = true;

    private final boolean processManagedPluginInfo = false;

    private final boolean processBuildInfo = true;

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
        logger.info( "Reading relationships for: {}\n  (from: {})", pomView.getRef(), source );

        try
        {
            final ProjectVersionRef projectRef = pomView.getRef();

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
            throw new CartoDataException( "Failed to parse version for model: {}. Reason: {}", e, pomView, e.getMessage() );
        }
        catch ( final IllegalArgumentException e )
        {
            throw new CartoDataException( "Failed to parse relationships for model: {}. Reason: {}", e, pomView, e.getMessage() );
        }
    }

    private void addExtensionUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        List<ExtensionView> extensions = null;
        try
        {
            extensions = pomView.getBuildExtensions();
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Cannot retrieve build extensions: {}", e, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot retrieve build extensions: {}", e, e.getMessage() );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( "Cannot retrieve build extensions: {}", e, e.getMessage() );
        }

        for ( final ExtensionView ext : extensions )
        {
            if ( ext == null )
            {
                continue;
            }

            try
            {
                final ProjectVersionRef ref = ext.asProjectVersionRef();

                // force the InvalidVersionSpecificationException.
                ref.getVersionSpec();

                builder.withExtensions( new ExtensionRelationship( source, projectRef, ref, builder.getNextExtensionIndex() ) );
            }
            catch ( final InvalidRefException e )
            {
                logger.error( "Build extension is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), ext.toXML() );
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                logger.error( "Build extension is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), ext.toXML() );
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( "Build extension is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), ext.toXML() );
            }
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

        List<PluginView> plugins = null;
        try
        {
            plugins = pomView.getAllPluginsMatching( "//plugin[artifactId/text()=\"maven-site-plugin\"]//reportPlugin" );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( "Cannot retrieve site-plugin nested reporting plugins: {}", e, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot retrieve site-plugin nested reporting plugins: {}", e, e.getMessage() );
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Cannot retrieve site-plugin nested reporting plugins: {}", e, e.getMessage() );
        }

        addPlugins( plugins, projectRef, builder, source, false );
    }

    public void addReportPluginUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        List<PluginView> plugins = null;
        try
        {
            plugins = pomView.getAllPluginsMatching( "//reporting/plugins/plugin" );
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Cannot retrieve reporting plugins: {}", e, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot retrieve reporting plugins: {}", e, e.getMessage() );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( "Cannot retrieve reporting plugins: {}", e, e.getMessage() );
        }

        addPlugins( plugins, projectRef, builder, source, false );
    }

    public void addBuildPluginUsages( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        if ( processManagedPluginInfo )
        {
            List<PluginView> plugins = null;
            try
            {
                plugins = pomView.getAllManagedBuildPlugins();
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( "Cannot retrieve managed plugins: {}", e, e.getMessage() );
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                logger.error( "Cannot retrieve managed plugins: {}", e, e.getMessage() );
            }
            catch ( final InvalidRefException e )
            {
                logger.error( "Cannot retrieve managed plugins: {}", e, e.getMessage() );
            }

            addPlugins( plugins, projectRef, builder, source, true );
        }

        List<PluginView> plugins = null;
        try
        {
            plugins = pomView.getAllBuildPlugins();
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Cannot retrieve build plugins: {}", e, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot retrieve build plugins: {}", e, e.getMessage() );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( "Cannot retrieve build plugins: {}", e, e.getMessage() );
        }

        addPlugins( plugins, projectRef, builder, source, false );
    }

    private void addPlugins( final List<PluginView> plugins, final ProjectVersionRef projectRef, final Builder builder, final URI source,
                             final boolean managed )
    {
        if ( plugins != null )
        {
            for ( final PluginView plugin : plugins )
            {
                ProjectVersionRef pluginRef = null;
                try
                {
                    if ( plugin.getVersion() == null )
                    {
                        logger.error( "Cannot find a version for plugin: {}. Skipping.", plugin.toXML() );
                        continue;
                    }

                    pluginRef = plugin.asProjectVersionRef();

                    // force the InvalidVersionSpecificationException.
                    pluginRef.getVersionSpec();

                    final String profileId = plugin.getProfileId();
                    final URI location = RelationshipUtils.profileLocation( profileId );

                    builder.withPlugins( new PluginRelationship( source, location, projectRef, pluginRef,
                                                                 builder.getNextPluginDependencyIndex( projectRef, managed ), managed ) );
                }
                catch ( final GalleyMavenException e )
                {
                    logger.error( "plugin is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), plugin.toXML() );
                    continue;
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    logger.error( "plugin is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), plugin.toXML() );
                    continue;
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( "plugin is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), plugin.toXML() );
                    continue;
                }

                List<PluginDependencyView> pluginDependencies = null;
                Set<PluginDependencyView> impliedPluginDependencies = null;
                try
                {
                    pluginDependencies = plugin.getLocalPluginDependencies();
                    impliedPluginDependencies = plugin.getImpliedPluginDependencies();
                }
                catch ( final GalleyMavenException e )
                {
                    logger.error( "Cannot retrieve plugin dependencies for: {}. Reason: {}", e, pluginRef, e.getMessage() );
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    logger.error( "Cannot retrieve plugin dependencies for: {}. Reason: {}", e, pluginRef, e.getMessage() );
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( "Cannot retrieve plugin dependencies for: {}. Reason: {}", e, pluginRef, e.getMessage() );
                }

                addPluginDependencies( pluginDependencies, plugin, pluginRef, projectRef, builder, source, managed );

                logger.info( "Adding implied dependencies for: {}\n\n  {}", pluginRef,
                             impliedPluginDependencies == null ? "-NONE-" : join( impliedPluginDependencies, "\n  " ) );
                addPluginDependencies( impliedPluginDependencies, plugin, pluginRef, projectRef, builder, source, managed );
            }
        }
    }

    private void addPluginDependencies( final Collection<PluginDependencyView> pluginDependencies, final PluginView plugin,
                                        final ProjectVersionRef pluginRef, final ProjectVersionRef projectRef, final Builder builder,
                                        final URI source, final boolean managed )
    {
        if ( pluginDependencies != null )
        {
            for ( final PluginDependencyView dep : pluginDependencies )
            {
                try
                {
                    final ProjectVersionRef ref = dep.asProjectVersionRef();

                    final String profileId = dep.getProfileId();
                    final URI location = RelationshipUtils.profileLocation( profileId );

                    final ArtifactRef artifactRef = new ArtifactRef( ref, dep.getType(), dep.getClassifier(), dep.isOptional() );

                    // force the InvalidVersionSpecificationException.
                    artifactRef.getVersionSpec();

                    builder.withPluginDependencies( new PluginDependencyRelationship( source, location, projectRef, pluginRef, artifactRef,
                                                                                      builder.getNextPluginDependencyIndex( pluginRef, managed ),
                                                                                      managed ) );
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( "plugin dependency is invalid in: {}! Reason: {}. Skipping:\n\n{}\n\n", e, pluginRef, e.getMessage(), dep.toXML() );
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    logger.error( "plugin dependency is invalid in: {}! Reason: {}. Skipping:\n\n{}\n\n", e, pluginRef, e.getMessage(), dep.toXML() );
                }
                catch ( final GalleyMavenException e )
                {
                    logger.error( "plugin dependency is invalid in: {}! Reason: {}. Skipping:\n\n{}\n\n", e, pluginRef, e.getMessage(), dep.toXML() );
                }
            }
        }
    }

    protected void addDependencyRelationships( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
    {
        if ( processManagedDependencyInfo )
        {
            List<DependencyView> deps = null;
            try
            {
                deps = pomView.getAllManagedDependencies();
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( "Failed to retrieve managed dependencies: {}. Skipping", e, e.getMessage() );
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                logger.error( "Failed to retrieve managed dependencies: {}. Skipping", e, e.getMessage() );
            }
            catch ( final InvalidRefException e )
            {
                logger.error( "Failed to retrieve managed dependencies: {}. Skipping", e, e.getMessage() );
            }

            addDependencies( deps, projectRef, builder, source, true );
        }

        // regardless of whether we're processing managed info, this is STRUCTURAL, so always grab it!
        List<DependencyView> boms = null;
        try
        {
            boms = pomView.getAllBOMs();
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Failed to retrieve BOM declarations: {}. Skipping", e, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Failed to retrieve BOM declarations: {}. Skipping", e, e.getMessage() );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( "Failed to retrieve BOM declarations: {}. Skipping", e, e.getMessage() );
        }

        addDependencies( boms, projectRef, builder, source, true );

        List<DependencyView> deps = null;
        try
        {
            deps = pomView.getAllDirectDependencies();
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Failed to retrieve direct dependencies: {}. Skipping", e, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Failed to retrieve direct dependencies: {}. Skipping", e, e.getMessage() );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( "Failed to retrieve direct dependencies: {}. Skipping", e, e.getMessage() );
        }

        addDependencies( deps, projectRef, builder, source, false );
    }

    private void addDependencies( final List<DependencyView> deps, final ProjectVersionRef projectRef, final Builder builder, final URI source,
                                  final boolean managed )
    {
        if ( deps != null )
        {
            for ( final DependencyView dep : deps )
            {
                try
                {
                    final ProjectVersionRef ref = dep.asProjectVersionRef();

                    final String profileId = dep.getProfileId();
                    final URI location = RelationshipUtils.profileLocation( profileId );

                    final ArtifactRef artifactRef = new ArtifactRef( ref, dep.getType(), dep.getClassifier(), dep.isOptional() );

                    // force the InvalidVersionSpecificationException.
                    artifactRef.getVersionSpec();

                    builder.withDependencies( new DependencyRelationship( source, location, projectRef, artifactRef, dep.getScope(),
                                                                          builder.getNextDependencyIndex( managed ), managed ) );
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( "dependency is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), dep.toXML() );
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    logger.error( "dependency is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), dep.toXML() );
                }
                catch ( final GalleyMavenException e )
                {
                    logger.error( "dependency is invalid! Reason: {}. Skipping:\n\n{}\n\n", e, e.getMessage(), dep.toXML() );
                }
            }
        }
    }

    protected void addParentRelationship( final URI source, final Builder builder, final MavenPomView pomView, final ProjectVersionRef projectRef )
    {
        try
        {
            final ParentView parent = pomView.getParent();
            if ( parent != null )
            {
                final ProjectVersionRef ref = parent.asProjectVersionRef();
                // force the InvalidVersionSpecificationException.
                ref.getVersionSpec();

                builder.withParent( new ParentRelationship( source, builder.getProjectRef(), ref ) );
            }
            else
            {
                builder.withParent( new ParentRelationship( source, builder.getProjectRef() ) );
            }
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Parent refernce is invalid! Reason: {}. Skipping.", e, e.getMessage() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Parent refernce is invalid! Reason: {}. Skipping.", e, e.getMessage() );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( "Parent refernce is invalid! Reason: {}. Skipping.", e, e.getMessage() );
        }
    }

}
