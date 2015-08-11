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
package org.commonjava.maven.cartographer.util;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.cartographer.request.*;
import org.commonjava.maven.cartographer.request.AbstractGraphRequest;
import org.commonjava.maven.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.spi.transport.LocationResolver;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class RecipeResolver
{

    @Inject
    private LocationResolver resolver;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private MavenPomReader pomReader;

    @Inject
    private PresetSelector presets;

    protected RecipeResolver()
    {
    }

    public RecipeResolver( final LocationResolver resolver, final LocationExpander locationExpander,
                        final DiscoverySourceManager sourceManager,
                        final MavenPomReader pomReader, final PresetSelector presets )
    {
        this.resolver = resolver;
        this.locationExpander = locationExpander;
        this.sourceManager = sourceManager;
        this.pomReader = pomReader;
        this.presets = presets;
    }

    public void resolve( final AbstractGraphRequest recipe )
                    throws CartoRequestException
    {
        if ( recipe == null )
        {
            return;
        }

        resolveSourceLocations( recipe );
        resolveDiscoveryConfig( recipe );
        resolvePresets( recipe );
        resolveVersionSelections( recipe );

        if ( recipe instanceof RepositoryContentRequest )
        {
            final RepositoryContentRequest rcr = (RepositoryContentRequest) recipe;
            final Set<String> excludedSources = rcr.getExcludedSources();
            final Set<Location> excludedLocations = resolveSourceLocationSet( excludedSources );
            rcr.setExcludedSourceLocations( excludedLocations );
        }

        recipe.normalize();
        if ( !recipe.isValid() )
        {
            throw new CartoRequestException( "Invalid repository request: {}", recipe );
        }
    }

    private void resolveDiscoveryConfig( final AbstractGraphRequest recipe )
                    throws CartoRequestException
    {
        if ( recipe.getDiscoveryConfig() == null )
        {
            final Location sourceLocation = recipe.getSourceLocation();
            if ( sourceLocation == null )
            {
                throw new CartoRequestException(
                                              "Source Location appears not to have been set on RepositoryContentRequest: {}. Cannot create DiscoveryConfig.",
                                              this );
            }

            final String uri = sourceLocation.getUri();

            DefaultDiscoveryConfig ddc;
            try
            {
                ddc = new DefaultDiscoveryConfig( uri );
            }
            catch ( final URISyntaxException e )
            {
                throw new CartoRequestException( "Invalid Source Location URI: {}. Cannot create DiscoveryConfig.", uri );
            }

            ddc.setEnabled( recipe.isResolve() );
            ddc.setEnabledPatchers( recipe.getPatcherIds() );
            ddc.setTimeoutMillis( 1000 * recipe.getTimeoutSecs() );

            resolveDiscoveryLocations( ddc, recipe );

            recipe.setDiscoveryConfig( ddc );
        }
    }

    private void resolveVersionSelections( final AbstractGraphRequest recipe )
                    throws CartoRequestException
    {
        final List<ProjectVersionRef> injectedBOMs = recipe.getInjectedBOMs();
        if ( injectedBOMs != null )
        {
            final List<? extends Location> locations = recipe.getDiscoveryConfig()
                                                             .getLocations();
            final Map<ProjectRef, ProjectVersionRef> injectedDepMgmt = recipe.getVersionSelections();
            readDepMgmtToVersionMap( injectedBOMs, locations, injectedDepMgmt );

            recipe.setVersionSelections( injectedDepMgmt );
        }

    }

    /**
     * Reads dependencyManagement from passed {@code boms} and add mapping for missing artifacts in the {@code versionMap}.
     * It reads the BOMs recursively each level at a time, it means once all passed BOMs are read, the next level (imported
     * BOMs into passed ones) is processed. This should be the same way as Maven does it.
     *
     * The method reads all the BOMs from the list of {@code locations} by {@link MavenPomReader}.
     *
     * @param boms the BOMs to read
     * @param locations locations where to look for the BOMs and their dependencies
     * @param versionMap target version map
     * @throws CartoRequestException if one of the BOMs does not exist or if its pom's dependencyManagement cannot be read correctly
     */
    private void readDepMgmtToVersionMap( final List<ProjectVersionRef> boms, final List<? extends Location> locations,
                                          final Map<ProjectRef, ProjectVersionRef> versionMap )
        throws CartoRequestException
    {
        final List<ProjectVersionRef> nextLevel = new ArrayList<ProjectVersionRef>();
        for ( final ProjectVersionRef bom : boms )
        {
            try
            {
                final MavenPomView bomView = pomReader.read( bom, locations );
                final List<DependencyView> managedDependencies = bomView.getManagedDependenciesNoImports();
                for ( final DependencyView managedDependency : managedDependencies )
                {
                    final ProjectRef ga = managedDependency.asProjectRef();
                    final ProjectVersionRef version = new ProjectVersionRef( ga, managedDependency.getVersion() );
                    if ( !versionMap.containsKey( ga ) )
                    {
                        versionMap.put( ga, version );
                    }
                }

                final List<DependencyView> importedBOMsViews = bomView.getAllBOMs();
                for ( final DependencyView importedBOMView : importedBOMsViews )
                {
                    if ( !nextLevel.contains( importedBOMView.asProjectVersionRef() ) )
                    {
                        nextLevel.add( importedBOMView.asProjectVersionRef() );
                    }
                }
            }
            catch ( final GalleyMavenException ex )
            {
                throw new CartoRequestException( "Error when trying to process BOM {}.", ex, bom );
            }
        }

        if ( !nextLevel.isEmpty() )
        {
            readDepMgmtToVersionMap( nextLevel, locations, versionMap );
        }
    }

    /**
     * Create one or more {@link Location} instances for the configured discovery source, according to the {@link DiscoverySourceManager} 
     * implementation's specific logic, if it hasn't already been done.
     * @throws CartoDataException 
     */
    public List<? extends Location> resolveDiscoveryLocations( final DiscoveryConfig config,
                                                               final AbstractGraphRequest recipe )
        throws CartoRequestException
    {
        List<? extends Location> locations = config.getLocations();
        if ( locations == null || locations.isEmpty() )
        {
            try
            {
                locations = sourceManager.createLocations( config.getDiscoverySource() );
            }
            catch ( CartoDataException e )
            {
                throw new CartoRequestException( "Failed to create locations for discovery source: %s. Reason: %s", e, config.getDiscoverySource(), e.getMessage() );
            }

            config.setLocations( locations );
        }
        else
        {
            final Location location = recipe.getSourceLocation();
            try
            {
                locations = locationExpander.expand( location );
                config.setLocations( locations );
            }
            catch ( final TransferException e )
            {
                throw new CartoRequestException( "Failed to expand potentially virtualized location: '%s'. Reason: %s", e,
                                              location, e.getMessage() );
            }
        }

        return locations;
    }

    public List<? extends Location> resolveDiscoveryLocations( final DiscoveryConfig config,
                                                               final URI discoverySource )
        throws CartoDataException
    {
        List<? extends Location> locations = config.getLocations();
        if ( locations == null || locations.isEmpty() )
        {
            locations = sourceManager.createLocations( discoverySource );
            config.setLocations( locations );
        }

        return locations;
    }

    public void resolveSourceLocations( final AbstractGraphRequest recipe )
        throws CartoRequestException
    {
        if ( recipe == null )
        {
            return;
        }

        final String spec = recipe.getSource();

        if ( spec == null )
        {
            return;
        }

        Location location;
        try
        {
            location = resolver.resolve( spec );
        }
        catch ( final TransferException e )
        {
            throw new CartoRequestException( "Failed to resolve location from spec: '%s'. Reason: %s", e, spec,
                                          e.getMessage() );
        }

        if ( location != null )
        {
            recipe.setSourceLocation( location );
        }
    }

    public void resolvePresets( final GraphComposition graphs )
    {
        if ( graphs == null )
        {
            return;
        }

        for ( final GraphDescription graph : graphs )
        {
            resolvePresets( graph );
        }
    }

    public void resolvePresets( final GraphDescription graph )
    {
        if ( graph == null )
        {
            return;
        }

        if ( graph.filter() == null )
        {
            final ProjectRelationshipFilter filter =
                presets.getPresetFilter( graph.getPreset(), graph.getDefaultPreset(), graph.getPresetParams() );

            graph.setFilter( filter );
        }
    }

    public void resolvePresets( final AbstractGraphRequest recipe )
    {
        if ( recipe == null )
        {
            return;
        }

        final GraphComposition comp = recipe.getGraphComposition();
        resolvePresets( comp );
    }

    public Set<Location> resolveSourceLocationSet( final Set<String> specs )
        throws CartoRequestException
    {
        final Set<Location> locations = new HashSet<Location>();
        if ( specs != null )
        {
            for ( final String spec : specs )
            {
                if ( spec == null )
                {
                    continue;
                }

                Location location;
                try
                {
                    location = resolver.resolve( spec );
                }
                catch ( final TransferException e )
                {
                    throw new CartoRequestException( "Failed to resolve location from spec: '%s'. Reason: %s", e, spec,
                                                  e.getMessage() );
                }

                if ( location != null )
                {
                    locations.add( location );
                }
            }
        }

        return locations;
    }

    public List<Location> resolveSourceLocationList( final List<String> specs )
        throws CartoDataException
    {
        final List<Location> locations = new ArrayList<Location>();
        if ( specs != null )
        {
            for ( final String spec : specs )
            {
                if ( spec == null )
                {
                    continue;
                }

                Location location;
                try
                {
                    location = resolver.resolve( spec );
                }
                catch ( final TransferException e )
                {
                    throw new CartoDataException( "Failed to resolve location from spec: '%s'. Reason: %s", e, spec,
                                                  e.getMessage() );
                }

                if ( location != null )
                {
                    locations.add( location );
                }
            }
        }

        return locations;
    }

}
