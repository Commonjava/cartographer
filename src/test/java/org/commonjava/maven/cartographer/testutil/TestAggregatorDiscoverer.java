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
package org.commonjava.maven.cartographer.testutil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
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
