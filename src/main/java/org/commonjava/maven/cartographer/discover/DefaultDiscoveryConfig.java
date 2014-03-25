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
package org.commonjava.maven.cartographer.discover;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.galley.model.Location;

public class DefaultDiscoveryConfig
    implements DiscoveryConfig
{

    private boolean enabled = true;

    private long timeoutMillis = TimeUnit.MILLISECONDS.convert( 60, TimeUnit.SECONDS );

    private final URI discoverySource;

    private List<? extends Location> discoveryLocations;

    private Set<String> patchers;

    private GraphMutator mutator;

    private boolean storeRelationships = true;

    private boolean includeManagedPlugins = false;

    private boolean includeBuildSection = true;

    private boolean includeManagedDependencies = true;

    public DefaultDiscoveryConfig( final URI discoverySource )
    {
        this.discoverySource = discoverySource;
    }

    public DefaultDiscoveryConfig( final String discoverySource )
        throws URISyntaxException
    {
        this.discoverySource = new URI( discoverySource );
    }

    public DefaultDiscoveryConfig( final Location location )
        throws URISyntaxException
    {
        this.discoverySource = new URI( location.getUri() );
        this.discoveryLocations = Collections.singletonList( location );
    }

    public DefaultDiscoveryConfig( final DiscoveryConfig discoveryConfig )
    {
        Set<String> enabledPatchers = discoveryConfig.getEnabledPatchers();
        if ( enabledPatchers == null )
        {
            enabledPatchers = new HashSet<String>();
        }
        else
        {
            enabledPatchers = new HashSet<String>( enabledPatchers );
        }

        this.patchers = enabledPatchers;
        this.enabled = discoveryConfig.isEnabled();
        this.timeoutMillis = discoveryConfig.getTimeoutMillis();
        this.discoverySource = discoveryConfig.getDiscoverySource();
        this.mutator = discoveryConfig.getMutator();
        this.discoveryLocations = discoveryConfig.getLocations();
        this.storeRelationships = discoveryConfig.isStoreRelationships();
        this.includeBuildSection = discoveryConfig.isIncludeBuildSection();
        this.includeManagedDependencies = discoveryConfig.isIncludeManagedDependencies();
        this.includeManagedPlugins = discoveryConfig.isIncludeManagedPlugins();
    }

    public DefaultDiscoveryConfig setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
        return this;
    }

    public DefaultDiscoveryConfig setTimeoutMillis( final long millis )
    {
        this.timeoutMillis = millis;
        return this;
    }

    public DefaultDiscoveryConfig setEnabledPatchers( final Set<String> patchers )
    {
        this.patchers = patchers;
        return this;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public long getTimeoutMillis()
    {
        return timeoutMillis;
    }

    @Override
    public URI getDiscoverySource()
    {
        return discoverySource;
    }

    @Override
    public Set<String> getEnabledPatchers()
    {
        return patchers == null ? Collections.<String> emptySet() : patchers;
    }

    @Override
    public GraphMutator getMutator()
    {
        return mutator == null ? new ManagedDependencyMutator() : mutator;
    }

    public DefaultDiscoveryConfig setMutator( final GraphMutator mutator )
    {
        this.mutator = mutator;
        return this;
    }

    @Override
    public List<? extends Location> getLocations()
    {
        return discoveryLocations;
    }

    @Override
    public void setLocations( final Collection<? extends Location> locations )
    {
        this.discoveryLocations = ( locations instanceof List ) ? (List<? extends Location>) locations : new ArrayList<Location>( locations );
    }

    @Override
    public boolean isStoreRelationships()
    {
        return storeRelationships;
    }

    @Override
    public void setStoreRelationships( final boolean store )
    {
        this.storeRelationships = store;
    }

    public void setIncludeManagedDependencies( final boolean include )
    {
        this.includeManagedDependencies = include;
    }

    public void setIncludeBuildSection( final boolean include )
    {
        this.includeBuildSection = include;
    }

    public void setIncludeManagedPlugins( final boolean include )
    {
        this.includeManagedPlugins = include;
    }

    @Override
    public boolean isIncludeBuildSection()
    {
        return includeBuildSection;
    }

    @Override
    public boolean isIncludeManagedDependencies()
    {
        return includeManagedDependencies;
    }

    @Override
    public boolean isIncludeManagedPlugins()
    {
        return includeManagedPlugins;
    }

}
