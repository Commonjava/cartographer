package org.commonjava.maven.cartographer.agg;

import java.net.URI;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;

public interface AggregationOptions
{

    ProjectRelationshipFilter getFilter();

    boolean processIncompleteSubgraphs();

    boolean processVariableSubgraphs();

    DiscoveryConfig getDiscoveryConfig();

    boolean isDiscoveryEnabled();

    long getDiscoveryTimeoutMillis();

    URI getDiscoverySource();

}
