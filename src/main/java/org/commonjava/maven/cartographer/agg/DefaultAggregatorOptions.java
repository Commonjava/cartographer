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
package org.commonjava.maven.cartographer.agg;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;

public class DefaultAggregatorOptions
    implements AggregationOptions
{

    private ProjectRelationshipFilter filter;

    private boolean processIncomplete = true;

    private boolean processVariable = true;

    private boolean discoveryEnabled = false;

    private URI discoverySource = RelationshipUtils.UNKNOWN_SOURCE_URI;

    // TODO: Revisit this default timeout!!!
    private long discoveryTimeoutMillis = TimeUnit.MILLISECONDS.convert( 10, TimeUnit.SECONDS );

    private DiscoveryConfig dc;

    public DefaultAggregatorOptions setFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
        return this;
    }

    public DefaultAggregatorOptions setProcessIncompleteSubgraphs( final boolean process )
    {
        this.processIncomplete = process;
        return this;
    }

    public DefaultAggregatorOptions setProcessVariableSubgraphs( final boolean process )
    {
        this.processVariable = process;
        return this;
    }

    public DefaultAggregatorOptions setDiscoveryEnabled( final boolean enabled )
    {
        this.discoveryEnabled = enabled;
        return this;
    }

    public DefaultAggregatorOptions setDiscoveryTimeoutMillis( final long millis )
    {
        this.discoveryTimeoutMillis = millis;
        return this;
    }

    public DefaultAggregatorOptions setDiscoverySource( final URI source )
    {
        this.discoverySource = source;
        return this;
    }

    @Override
    public ProjectRelationshipFilter getFilter()
    {
        return filter == null ? new AnyFilter() : filter;
    }

    @Override
    public boolean processIncompleteSubgraphs()
    {
        return processIncomplete;
    }

    @Override
    public boolean processVariableSubgraphs()
    {
        return processVariable;
    }

    @Override
    public DiscoveryConfig getDiscoveryConfig()
    {
        return dc == null ? new DefaultDiscoveryConfig( discoverySource ).setEnabled( discoveryEnabled )
                                                                         .setTimeoutMillis( discoveryTimeoutMillis ) : dc;
    }

    @Override
    public boolean isDiscoveryEnabled()
    {
        return discoveryEnabled;
    }

    @Override
    public long getDiscoveryTimeoutMillis()
    {
        return discoveryTimeoutMillis;
    }

    @Override
    public URI getDiscoverySource()
    {
        return discoverySource;
    }

    @Override
    public String toString()
    {
        return String.format( "DefaultAggregatorOptions [\n\tprocessIncomplete=%s" + "\n\tprocessVariable=%s" + "\n\tdiscoveryEnabled=%s"
                                  + "\n\tdiscoveryTimeoutMillis=%s" + "\n\n\tfilter:\n\n%s\n\n]", processIncomplete, processVariable,
                              discoveryEnabled,
                              discoveryTimeoutMillis, filter );
    }

    public DefaultAggregatorOptions setDiscoveryConfig( final DiscoveryConfig dc )
    {
        this.dc = dc;
        this.discoverySource = dc.getDiscoverySource();
        this.discoveryEnabled = dc.isEnabled();
        this.discoveryTimeoutMillis = dc.getTimeoutMillis();
        return this;
    }

}
