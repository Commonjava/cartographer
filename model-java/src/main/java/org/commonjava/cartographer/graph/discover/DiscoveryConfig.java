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
package org.commonjava.cartographer.graph.discover;

import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig;
import org.commonjava.maven.galley.model.Location;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiscoveryConfig
{

    private boolean enabled = true;

    private long timeoutMillis = TimeUnit.MILLISECONDS.convert( 60, TimeUnit.SECONDS );

    private final URI discoverySource;

    private List<? extends Location> discoveryLocations;

    private Collection<String> patchers;

    private boolean storeRelationships = true;

    private boolean includeManagedPlugins = false;

    private boolean includeBuildSection = true;

    private boolean includeManagedDependencies = true;

    public static DiscoveryConfig getDisabledConfig()
    {
        return new DiscoveryConfig();
    }

    private DiscoveryConfig()
    {
        this.enabled = false;
        this.discoverySource = null;
    }

    public DiscoveryConfig( final URI discoverySource )
    {
        this.discoverySource = discoverySource;
    }

    public DiscoveryConfig( final String discoverySource )
            throws URISyntaxException
    {
        this.discoverySource = new URI( discoverySource );
    }

    public DiscoveryConfig( final Location location )
            throws URISyntaxException
    {
        this.discoverySource = new URI( location.getUri() );
        this.discoveryLocations = Collections.singletonList( location );
    }

    public DiscoveryConfig( final DiscoveryConfig discoveryConfig )
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
        this.discoveryLocations = discoveryConfig.getLocations();
        this.storeRelationships = discoveryConfig.isStoreRelationships();
        this.includeBuildSection = discoveryConfig.isIncludeBuildSection();
        this.includeManagedDependencies = discoveryConfig.isIncludeManagedDependencies();
        this.includeManagedPlugins = discoveryConfig.isIncludeManagedPlugins();
    }

    public DiscoveryConfig setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
        return this;
    }

    public DiscoveryConfig setTimeoutMillis( final long millis )
    {
        this.timeoutMillis = millis;
        return this;
    }

    public DiscoveryConfig setEnabledPatchers( final Collection<String> patchers )
    {
        this.patchers = patchers;
        return this;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public long getTimeoutMillis()
    {
        return timeoutMillis;
    }

    public URI getDiscoverySource()
    {
        return discoverySource;
    }

    public Collection<String> getEnabledPatchers()
    {
        return patchers == null ? Collections.<String>emptySet() : patchers;
    }

    public List<? extends Location> getLocations()
    {
        return discoveryLocations;
    }

    public void setLocations( final Collection<? extends Location> locations )
    {
        this.discoveryLocations = ( locations instanceof List ) ?
                (List<? extends Location>) locations :
                new ArrayList<Location>( locations );
    }

    public boolean isStoreRelationships()
    {
        return storeRelationships;
    }

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

    public boolean isIncludeBuildSection()
    {
        return includeBuildSection;
    }

    public boolean isIncludeManagedDependencies()
    {
        return includeManagedDependencies;
    }

    public boolean isIncludeManagedPlugins()
    {
        return includeManagedPlugins;
    }

    public ModelProcessorConfig getProcessorConfig()
    {
        return new ModelProcessorConfig().setIncludeBuildSection( includeBuildSection )
                                         .setIncludeManagedDependencies( includeManagedDependencies )
                                         .setIncludeManagedPlugins( includeManagedPlugins );
    }
}
