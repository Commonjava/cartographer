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

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.cdmckay.coffeedom.CoffeeDOMException;
import org.cdmckay.coffeedom.Document;
import org.cdmckay.coffeedom.Element;
import org.cdmckay.coffeedom.input.SAXBuilder;
import org.cdmckay.coffeedom.xpath.XPath;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships.Builder;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class MavenModelProcessor
{

    private static final String SITE_PLUGIN = "org.apache.maven.plugins:maven-site-plugin";

    private static final Logger logger = new Logger( MavenModelProcessor.class );

    @Inject
    private CartoDataManager dataManager;

    protected MavenModelProcessor()
    {
    }

    public MavenModelProcessor( final CartoDataManager dataManager )
    {
        this.dataManager = dataManager;
    }

    public DiscoveryResult storeModelRelationships( final Model model, final URI source )
        throws CartoDataException
    {
        final DiscoveryResult fromRead = readRelationships( model, source );
        final ProjectVersionRef projectRef = fromRead.getSelectedRef();
        dataManager.clearErrors( projectRef );
        final Set<ProjectRelationship<?>> skipped = dataManager.storeRelationships( fromRead.getAllDiscoveredRelationships() );

        return new DiscoveryResult( source, fromRead, skipped );

    }

    public DiscoveryResult readRelationships( final Model model, final URI source )
        throws CartoDataException
    {
        try
        {
            String g = model.getGroupId();
            String v = model.getVersion();

            final Parent parent = model.getParent();
            if ( parent != null )
            {
                if ( g == null )
                {
                    g = parent.getGroupId();
                }

                if ( v == null )
                {
                    v = parent.getVersion();
                }
            }

            v = resolveExpressions( v, model );
            g = resolveExpressions( g, model );

            String a = model.getArtifactId();

            a = resolveExpressions( a, model );

            final ProjectVersionRef projectRef = new ProjectVersionRef( g, a, v );

            final EProjectDirectRelationships.Builder builder = new EProjectDirectRelationships.Builder( source, projectRef );

            addParentRelationship( source, builder, model, projectRef );
            addExtensionUsages( source, builder, model, projectRef );

            addDependencyRelationships( source, builder, model, model, RelationshipUtils.POM_ROOT_URI, projectRef );

            addPluginUsages( source, builder, model.getBuild(), model.getReporting(), model, RelationshipUtils.POM_ROOT_URI, projectRef );

            final List<Profile> profiles = model.getProfiles();
            if ( profiles != null )
            {
                for ( final Profile profile : profiles )
                {
                    final URI location = RelationshipUtils.profileLocation( profile.getId() );

                    addDependencyRelationships( source, builder, profile, model, location, projectRef );

                    addPluginUsages( source, builder, profile.getBuild(), profile.getReporting(), model, location, projectRef );
                }
            }

            final EProjectDirectRelationships rels = builder.build();
            return new DiscoveryResult( source, projectRef, rels.getAllRelationships() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new CartoDataException( "Failed to parse version string: '%s' for model: %s. Reason: %s", e, model.getVersion(), model,
                                          e.getMessage() );
        }
    }

    private String resolveExpressions( final String raw, final Model model )
        throws CartoDataException
    {
        if ( raw == null )
        {
            return raw;
        }

        if ( raw.contains( "${" ) )
        {
            final StringSearchInterpolator interp = new StringSearchInterpolator();
            interp.addValueSource( new PropertiesBasedValueSource( model.getProperties() ) );

            final List<String> expressionRoots = new ArrayList<String>();
            expressionRoots.add( "pom." );
            expressionRoots.add( "project." );

            interp.addValueSource( new PrefixedObjectValueSource( expressionRoots, model, true ) );

            try
            {
                return interp.interpolate( raw );
            }
            catch ( final InterpolationException e )
            {
                throw new CartoDataException( "Failed to resolve expression from model.\nRaw string: '%s'\nModel: %s\nError: %s", e, raw, model,
                                              e.getMessage() );
            }
        }

        return raw;
    }

    private void addExtensionUsages( final URI source, final Builder builder, final Model model, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        final List<Extension> extensions = model.getBuild() == null ? new ArrayList<Extension>() : model.getBuild()
                                                                                                        .getExtensions();

        for ( final Extension ext : extensions )
        {
            if ( ext == null )
            {
                continue;
            }

            String v = ext.getVersion();
            if ( isEmpty( ext.getVersion() ) )
            {
                v = "[0,]";
                continue;
            }

            final String g = resolveExpressions( ext.getGroupId(), model );
            final String a = resolveExpressions( ext.getArtifactId(), model );
            v = resolveExpressions( ext.getVersion(), model );
            final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );

            //            if ( isValid( ref ) )
            //            {
            builder.withExtensions( new ExtensionRelationship( source, projectRef, ref, builder.getNextExtensionIndex() ) );
            //            }
        }
    }

    private void addPluginUsages( final URI source, final Builder builder, final BuildBase build, final Reporting reporting, final Model model,
                                  final URI location, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        addPluginUsages( source, builder, build, model, false, location, projectRef );
        addPluginUsages( source, builder, build, model, true, location, projectRef );
        addReportPluginUsages( source, builder, reporting, model, location, projectRef );
        addSiteReportPluginUsages( source, builder, build, model, location, projectRef );
    }

    private void addSiteReportPluginUsages( final URI source, final Builder builder, final BuildBase build, final Model model, final URI location,
                                            final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        if ( build != null )
        {
            List<Plugin> plugins = build.getPlugins();
            for ( final Plugin plugin : plugins )
            {
                if ( plugin.getKey()
                           .equals( SITE_PLUGIN ) )
                {
                    addSiteReportPlugins( source, builder, plugin, plugin.getConfiguration(), projectRef, model, location );
                }
            }

            final PluginManagement pmgmt = build.getPluginManagement();
            if ( pmgmt != null )
            {
                plugins = pmgmt.getPlugins();
                for ( final Plugin plugin : plugins )
                {
                    if ( plugin.getKey()
                               .equals( SITE_PLUGIN ) )
                    {
                        addSiteReportPlugins( source, builder, plugin, plugin.getConfiguration(), projectRef, model, location );
                    }
                }
            }
        }
    }

    public void addSiteReportPlugins( final URI source, final Builder builder, final Plugin plugin, final Object configuration,
                                      final ProjectVersionRef projectRef, final Model model, final URI location )
        throws CartoDataException
    {
        XPath reportPluginPath = null;
        try
        {
            reportPluginPath = XPath.newInstance( "configuration/reportPlugins/*" );
        }
        catch ( final CoffeeDOMException e )
        {
            logger.error( "Cannot compile report-plugin selection XPath for site-plugin configuration. Error: " + e.getMessage(), e );
        }

        if ( reportPluginPath == null || configuration == null )
        {
            return;
        }

        final String xml = configuration.toString();
        try
        {
            final Document configDoc = new SAXBuilder().build( new ByteArrayInputStream( xml.getBytes() ) );

            final List<Object> pluginNodes = reportPluginPath.selectNodes( configDoc );

            if ( pluginNodes != null )
            {
                for ( final Object node : pluginNodes )
                {
                    final Element pluginNode = (Element) node;
                    final Element gEl = pluginNode.getChild( "groupId" );
                    final Element aEl = pluginNode.getChild( "artifactId" );
                    final Element vEl = pluginNode.getChild( "version" );

                    if ( gEl == null || aEl == null )
                    {
                        logger.debug( "Cannot process invalid report-plugin configuration: %s:%s", gEl, aEl );
                        continue;
                    }

                    String gid = gEl.getTextNormalize();
                    String aid = aEl.getTextNormalize();

                    String ver = null;
                    if ( vEl != null )
                    {
                        ver = vEl.getTextNormalize();
                    }

                    if ( isEmpty( ver ) )
                    {
                        ver = "[0,]";
                        logger.debug( "No version found for: %s:%s. Skipping.", gEl, aEl );
                        continue;
                    }

                    gid = resolveExpressions( gid, model );
                    aid = resolveExpressions( aid, model );
                    ver = resolveExpressions( ver, model );

                    if ( containsNull( gid, aid, ver ) )
                    {
                        logger.info( "Incomplete GAV for site-report plugin: %s:%s:%s", gid, aid, ver );
                        continue;
                    }

                    final ProjectVersionRef ref = new ProjectVersionRef( gid, aid, ver );

                    if ( isValid( ref ) )
                    {
                        builder.withPlugins( new PluginRelationship( source, location, projectRef, ref, builder.getNextPluginIndex( false ), false ) );
                    }
                }
            }
        }
        catch ( final CoffeeDOMException e )
        {
            logger.error( "Cannot select report plugin definitions from site-plugin configuration. Error: " + e.getMessage(), e );
        }
        catch ( final IOException e )
        {
            logger.error( "Cannot read site-plugin configuration. Error: " + e.getMessage(), e );
        }
    }

    public void addReportPluginUsages( final URI source, final Builder builder, final Reporting reporting, final Model model, final URI location,
                                       final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        if ( reporting != null )
        {
            final List<ReportPlugin> plugins = reporting.getPlugins();
            for ( final ReportPlugin plugin : plugins )
            {
                if ( plugin == null )
                {
                    continue;
                }

                String v = plugin.getVersion();
                if ( isEmpty( plugin.getVersion() ) )
                {
                    v = "[0,]";
                    continue;
                }

                final String g = resolveExpressions( plugin.getGroupId(), model );
                final String a = resolveExpressions( plugin.getArtifactId(), model );
                v = resolveExpressions( plugin.getVersion(), model );

                if ( containsNull( g, a, v ) )
                {
                    logger.info( "Incomplete GAV for report plugin: %s:%s:%s", g, a, v );
                    continue;
                }

                final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );

                if ( isValid( ref ) )
                {
                    builder.withPlugins( new PluginRelationship( source, location, projectRef, ref, builder.getNextPluginDependencyIndex( projectRef,
                                                                                                                                          false ),
                                                                 false ) );
                }
            }
        }
    }

    public void addPluginUsages( final URI source, final Builder builder, final BuildBase build, final Model model, final boolean managed,
                                 final URI location, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        final List<Plugin> plugins = getPlugins( model, managed );

        for ( final Plugin plugin : plugins )
        {
            if ( plugin == null )
            {
                continue;
            }

            String v = plugin.getVersion();
            if ( isEmpty( plugin.getVersion() ) )
            {
                v = "[0,]";
                continue;
            }

            final String g = resolveExpressions( plugin.getGroupId(), model );
            final String a = resolveExpressions( plugin.getArtifactId(), model );
            v = resolveExpressions( plugin.getVersion(), model );

            if ( containsNull( g, a, v ) )
            {
                logger.info( "Incomplete GAV for plugin: %s:%s:%s", g, a, v );
                continue;
            }

            final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );

            if ( isValid( ref ) )
            {
                builder.withPlugins( new PluginRelationship( source, location, projectRef, ref, builder.getNextPluginDependencyIndex( projectRef,
                                                                                                                                      managed ),
                                                             managed ) );
            }
        }
    }

    private List<Plugin> getPlugins( final Model model, final boolean managed )
    {
        final Build build = model.getBuild();
        if ( build == null )
        {
            return Collections.emptyList();
        }

        List<Plugin> plugins = null;
        if ( managed )
        {
            final PluginManagement pmgmt = build.getPluginManagement();
            if ( pmgmt != null )
            {
                plugins = pmgmt.getPlugins();
            }
        }
        else
        {
            plugins = build.getPlugins();
        }

        if ( plugins == null )
        {
            return Collections.emptyList();
        }

        return plugins;
    }

    public void addDependencyRelationships( final URI source, final Builder builder, final ModelBase base, final Model model, final URI location,
                                            final ProjectVersionRef projectRef )
        throws CartoDataException, InvalidVersionSpecificationException
    {
        addDependencyRelationships( source, builder, base, model, true, location, projectRef );
        addDependencyRelationships( source, builder, base, model, false, location, projectRef );
    }

    public void addDependencyRelationships( final URI source, final Builder builder, final ModelBase base, final Model model, final boolean managed,
                                            final URI location, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        if ( managed )
        {
            final DependencyManagement dm = base.getDependencyManagement();
            if ( dm != null )
            {
                for ( final Dependency dep : dm.getDependencies() )
                {
                    final String g = resolveExpressions( dep.getGroupId(), model );
                    final String a = resolveExpressions( dep.getArtifactId(), model );
                    final String v = resolveExpressions( dep.getVersion(), model );

                    if ( containsNull( g, a, v ) )
                    {
                        logger.info( "Incomplete GAV for dep: %s", dep );
                        continue;
                    }

                    final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );

                    final ArtifactRef artifactRef = new ArtifactRef( ref, dep.getType(), dep.getClassifier(), dep.isOptional() );

                    if ( isValid( artifactRef ) )
                    {
                        builder.withDependencies( new DependencyRelationship( source, location, projectRef, artifactRef,
                                                                              DependencyScope.getScope( dep.getScope() ),
                                                                              builder.getNextDependencyIndex( true ), true ) );
                    }
                }
            }
        }
        else
        {
            for ( final Dependency dep : base.getDependencies() )
            {
                final String g = resolveExpressions( dep.getGroupId(), model );
                final String a = resolveExpressions( dep.getArtifactId(), model );
                final String v = resolveExpressions( dep.getVersion(), model );

                if ( containsNull( g, a, v ) )
                {
                    logger.info( "Incomplete GAV for dep: %s", dep );
                    continue;
                }

                final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );

                final ArtifactRef artifactRef = new ArtifactRef( ref, dep.getType(), dep.getClassifier(), dep.isOptional() );

                if ( isValid( artifactRef ) )
                {
                    builder.withDependencies( new DependencyRelationship( source, location, projectRef, artifactRef,
                                                                          DependencyScope.getScope( dep.getScope() ),
                                                                          builder.getNextDependencyIndex( false ), false ) );
                }
            }
        }
    }

    private boolean containsNull( final String... values )
    {
        for ( final String value : values )
        {
            if ( value == null )
            {
                return true;
            }
        }

        return false;
    }

    public void addParentRelationship( final URI source, final Builder builder, final Model model, final ProjectVersionRef projectRef )
        throws CartoDataException
    {
        final Parent parent = model.getParent();
        if ( parent != null )
        {
            final String g = resolveExpressions( parent.getGroupId(), model );
            final String a = resolveExpressions( parent.getArtifactId(), model );
            final String v = resolveExpressions( parent.getVersion(), model );

            final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );
            if ( isValid( ref ) )
            {
                builder.withParent( new ParentRelationship( source, builder.getProjectRef(), ref ) );
            }
        }
        else
        {
            builder.withParent( new ParentRelationship( source, builder.getProjectRef() ) );
        }
    }

    private boolean isValid( final ProjectVersionRef ref )
    {
        boolean valid = true;
        try
        {
            ref.getVersionSpec();
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version: %s. Reason: %s", e, ref, e.getMessage() );
            valid = false;
        }

        return valid;
    }

}
