/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
