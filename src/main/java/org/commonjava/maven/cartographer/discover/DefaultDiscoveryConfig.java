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
package org.commonjava.maven.cartographer.discover;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    private Collection<String> patchers;

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
        Collection<String> enabledPatchers = discoveryConfig.getEnabledPatchers();
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

    public DefaultDiscoveryConfig setEnabledPatchers( final Collection<String> patchers )
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
    public Collection<String> getEnabledPatchers()
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
        this.discoveryLocations =
            ( locations instanceof List ) ? (List<? extends Location>) locations : new ArrayList<Location>( locations );
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
