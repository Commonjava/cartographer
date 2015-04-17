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
package org.commonjava.maven.cartographer.agg;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.preset.ScopedProjectFilter;

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

    private GraphMutator mutator;

    public DefaultAggregatorOptions()
    {
        this.filter = new ScopedProjectFilter();
    }

    public DefaultAggregatorOptions( final AggregationOptions options, final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
        this.processIncomplete = options.processIncompleteSubgraphs();
        this.processVariable = options.processVariableSubgraphs();
        this.discoveryEnabled = options.isDiscoveryEnabled();
        this.discoverySource = options.getDiscoverySource();
        this.discoveryTimeoutMillis = options.getDiscoveryTimeoutMillis();
        this.dc = options.getDiscoveryConfig();
        this.mutator = options.getMutator();
    }

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

    public DefaultAggregatorOptions setMutator( final GraphMutator mutator )
    {
        this.mutator = mutator;
        return this;
    }

    @Override
    public ProjectRelationshipFilter getFilter()
    {
        return filter == null ? AnyFilter.INSTANCE : filter;
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
                                                                         .setTimeoutMillis( discoveryTimeoutMillis )
                                                                         .setMutator( mutator ) : dc;
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
        return String.format( "DefaultAggregatorOptions [\n\tmutator=%s\n\tprocessIncomplete=%s"
                                  + "\n\tprocessVariable=%s" + "\n\tdiscoveryEnabled=%s"
                                  + "\n\tdiscoveryTimeoutMillis=%s" + "\n\n\tfilter:\n\n%s\n\n]", mutator,
                              processIncomplete, processVariable, discoveryEnabled,
                              discoveryTimeoutMillis, filter );
    }

    public DefaultAggregatorOptions setDiscoveryConfig( final DiscoveryConfig dc )
    {
        this.dc = dc;
        this.discoverySource = dc.getDiscoverySource();
        this.discoveryEnabled = dc.isEnabled();
        this.discoveryTimeoutMillis = dc.getTimeoutMillis();
        this.mutator = dc.getMutator();
        return this;
    }

    @Override
    public GraphMutator getMutator()
    {
        return mutator == null ? new ManagedDependencyMutator() : mutator;
    }

}
