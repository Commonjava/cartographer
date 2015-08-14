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
package org.commonjava.cartographer.testutil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.cartographer.spi.graph.discover.ProjectRelationshipDiscoverer;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAggregatorDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<ProjectVersionRef, DiscoveryResult> mappedResults =
        new HashMap<ProjectVersionRef, DiscoveryResult>();

    private final Set<ProjectVersionRef> seen = new HashSet<ProjectVersionRef>();

    public void mapResult( final ProjectVersionRef ref, final DiscoveryResult result )
    {
        mappedResults.put( ref, result );
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        return ref;
    }

    public boolean sawDiscovery( final ProjectVersionRef ref )
    {
        return seen.contains( ref );
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final RelationshipGraph graph,
                                                  final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        seen.add( ref );

        final DiscoveryResult result = mappedResults.get( ref );
        logger.info( "DISCOVER: {}....\n  {}", ref, result );
        if ( result != null && discoveryConfig.isStoreRelationships() )
        {
            try
            {
                graph.storeRelationships( result.getAllDiscoveredRelationships() );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Failed to store relationships for: {} in: {}. Reason: {}", e, ref,
                                              graph, e.getMessage() );
            }
        }

        return result;
    }

}
