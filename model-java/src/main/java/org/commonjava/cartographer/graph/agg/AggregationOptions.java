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
package org.commonjava.cartographer.graph.agg;

import org.commonjava.cartographer.graph.filter.AnyFilter;
import org.commonjava.cartographer.graph.filter.ProjectRelationshipFilter;
import org.commonjava.cartographer.graph.util.RelationshipUtils;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.cartographer.graph.preset.ScopedProjectFilter;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.commonjava.maven.atlas.graph.rel.RelationshipConstants.UNKNOWN_SOURCE_URI;

public class AggregationOptions
{

    private ProjectRelationshipFilter filter;

    private boolean processIncomplete = true;

    private boolean processVariable = true;

    private boolean discoveryEnabled = false;

    private URI discoverySource = UNKNOWN_SOURCE_URI;

    // TODO: Revisit this default timeout!!!
    private long discoveryTimeoutMillis = TimeUnit.MILLISECONDS.convert( 10, TimeUnit.SECONDS );

    private DiscoveryConfig dc;

    public AggregationOptions()
    {
        this.filter = new ScopedProjectFilter();
    }

    public AggregationOptions( final AggregationOptions options, final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
        this.processIncomplete = options.processIncompleteSubgraphs();
        this.processVariable = options.processVariableSubgraphs();
        this.discoveryEnabled = options.isDiscoveryEnabled();
        this.discoverySource = options.getDiscoverySource();
        this.discoveryTimeoutMillis = options.getDiscoveryTimeoutMillis();
        this.dc = options.getDiscoveryConfig();
    }

    public AggregationOptions setFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
        return this;
    }

    public AggregationOptions setProcessIncompleteSubgraphs( final boolean process )
    {
        this.processIncomplete = process;
        return this;
    }

    public AggregationOptions setProcessVariableSubgraphs( final boolean process )
    {
        this.processVariable = process;
        return this;
    }

    public AggregationOptions setDiscoveryEnabled( final boolean enabled )
    {
        this.discoveryEnabled = enabled;
        return this;
    }

    public AggregationOptions setDiscoveryTimeoutMillis( final long millis )
    {
        this.discoveryTimeoutMillis = millis;
        return this;
    }

    public AggregationOptions setDiscoverySource( final URI source )
    {
        this.discoverySource = source;
        return this;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter == null ? AnyFilter.INSTANCE : filter;
    }

    public boolean processIncompleteSubgraphs()
    {
        return processIncomplete;
    }

    public boolean processVariableSubgraphs()
    {
        return processVariable;
    }

    public DiscoveryConfig getDiscoveryConfig()
    {
        return dc == null ? new DiscoveryConfig( discoverySource ).setEnabled( discoveryEnabled )
                                                                  .setTimeoutMillis( discoveryTimeoutMillis )
                          : dc;
    }

    public boolean isDiscoveryEnabled()
    {
        return discoveryEnabled;
    }

    public long getDiscoveryTimeoutMillis()
    {
        return discoveryTimeoutMillis;
    }

    public URI getDiscoverySource()
    {
        return discoverySource;
    }

    @Override
    public String toString()
    {
        return String.format( "AggregationOptions [\n\tprocessIncomplete=%s"
                                  + "\n\tprocessVariable=%s" + "\n\tdiscoveryEnabled=%s"
                                  + "\n\tdiscoveryTimeoutMillis=%s" + "\n\n\tfilter:\n\n%s\n\n]",
                              processIncomplete, processVariable, discoveryEnabled,
                              discoveryTimeoutMillis, filter );
    }

    public AggregationOptions setDiscoveryConfig( final DiscoveryConfig dc )
    {
        this.dc = dc;
        this.discoverySource = dc.getDiscoverySource();
        this.discoveryEnabled = dc.isEnabled();
        this.discoveryTimeoutMillis = dc.getTimeoutMillis();
        return this;
    }

}
