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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.commonjava.maven.atlas.graph.mutate.GraphMutator;

public class DefaultDiscoveryConfig
    implements DiscoveryConfig
{

    private boolean enabled = true;

    private long timeoutMillis = TimeUnit.MILLISECONDS.convert( 60, TimeUnit.SECONDS );

    private final URI discoverySource;

    private Set<String> patchers;

    private GraphMutator mutator;

    public DefaultDiscoveryConfig( final URI discoverySource )
    {
        this.discoverySource = discoverySource;
    }

    public DefaultDiscoveryConfig( final String discoverySource )
        throws URISyntaxException
    {
        this.discoverySource = new URI( discoverySource );
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
        return mutator;
    }

    public DefaultDiscoveryConfig setMutator( final GraphMutator mutator )
    {
        this.mutator = mutator;
        return this;
    }

}
