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

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
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

    GraphMutator getMutator();

}
