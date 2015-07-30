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
package org.commonjava.maven.cartographer.dto.resolve;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.dto.AbstractResolverRecipe;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationResolver;

public class DTOResolver
{

    @Inject
    private LocationResolver resolver;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private MavenPomReader pomReader;

    @Inject
    private PresetSelector presets;

    protected DTOResolver()
    {
    }

    public DTOResolver( final LocationResolver resolver, final DiscoverySourceManager sourceManager,
                        final MavenPomReader pomReader, final PresetSelector presets )
    {
        this.resolver = resolver;
        this.sourceManager = sourceManager;
        this.pomReader = pomReader;
        this.presets = presets;
    }

    public void resolve( final AbstractResolverRecipe recipe )
        throws CartoDataException
    {
        if ( recipe == null )
        {
            return;
        }

        resolveSourceLocation( recipe );
        resolvePresets( recipe );
        resolveDiscoveryConfig( recipe );
        resolveVersionSelections( recipe );

        if ( recipe instanceof RepositoryContentRecipe )
        {
            final RepositoryContentRecipe rcr = (RepositoryContentRecipe) recipe;
            final Set<String> excludedSources = rcr.getExcludedSources();
            final Set<Location> excludedLocations = resolveSourceLocationSet( excludedSources );
            rcr.setExcludedSourceLocations( excludedLocations );
        }
    }

    private void resolveDiscoveryConfig( final AbstractResolverRecipe recipe )
        throws CartoDataException
    {
        if ( recipe.getDiscoveryConfig() == null )
        {
            final Location sourceLocation = recipe.getSourceLocation();
            if ( sourceLocation == null )
            {
                throw new CartoDataException(
                                              "Source Location appears not to have been set on RepositoryContentRecipe: {}. Cannot create DiscoveryConfig.",
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
                throw new CartoDataException( "Invalid Source Location URI: {}. Cannot create DiscoveryConfig.", uri );
            }

            ddc.setEnabled( recipe.isResolve() );
            ddc.setEnabledPatchers( recipe.getPatcherIds() );
            ddc.setTimeoutMillis( 1000 * recipe.getTimeoutSecs() );

            recipe.setDiscoveryConfig( ddc );
        }
    }

    private void resolveVersionSelections( final AbstractResolverRecipe recipe )
        throws CartoDataException
    {
        final List<ProjectVersionRef> injectedBOMs = recipe.getInjectedBOMs();
        if ( injectedBOMs != null )
        {
            final List<? extends Location> locations = initDiscoveryLocations( recipe.getDiscoveryConfig() );

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
     * @throws CartoDataException if one of the BOMs does not exist or if its pom's dependencyManagement cannot be read correctly
     */
    private void readDepMgmtToVersionMap( final List<ProjectVersionRef> boms, final List<? extends Location> locations,
                                          final Map<ProjectRef, ProjectVersionRef> versionMap )
        throws CartoDataException
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
                throw new CartoDataException( "Error when trying to process BOM {}.", ex, bom );
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
    public List<? extends Location> initDiscoveryLocations( final DiscoveryConfig config )
        throws CartoDataException
    {
        List<? extends Location> locations = config.getLocations();
        if ( locations == null || locations.isEmpty() )
        {
            locations = sourceManager.createLocations( config.getDiscoverySource() );
            config.setLocations( locations );
        }

        return locations;
    }

    public void resolveSourceLocation( final AbstractResolverRecipe recipe )
        throws CartoDataException
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
            throw new CartoDataException( "Failed to resolve location from spec: '%s'. Reason: %s", e, spec,
                                          e.getMessage() );
        }

        recipe.setSourceLocation( location );
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

    public void resolvePresets( final AbstractResolverRecipe recipe )
    {
        if ( recipe == null )
        {
            return;
        }

        final GraphComposition comp = recipe.getGraphComposition();
        resolvePresets( comp );
    }

    public Set<Location> resolveSourceLocationSet( final Set<String> specs )
        throws CartoDataException
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
