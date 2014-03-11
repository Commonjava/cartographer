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
package org.commonjava.maven.cartographer.testutil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAggregatorDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<ProjectVersionRef, DiscoveryResult> mappedResults = new HashMap<ProjectVersionRef, DiscoveryResult>();

    private final Set<ProjectVersionRef> seen = new HashSet<ProjectVersionRef>();

    private final CartoDataManager data;

    public TestAggregatorDiscoverer( final CartoDataManager data )
    {
        this.data = data;
    }

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

    /**
     * @deprecated Use {@link #discoverRelationships(ProjectVersionRef,DiscoveryConfig)} instead
     */
    @Deprecated
    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig, final boolean storeRelationships )
        throws CartoDataException
    {
        discoveryConfig.setStoreRelationships( storeRelationships );
        return discoverRelationships( ref, discoveryConfig );
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        seen.add( ref );

        final DiscoveryResult result = mappedResults.get( ref );
        logger.info( "DISCOVER: {}....\n  {}", ref, result );
        if ( result != null && discoveryConfig.isStoreRelationships() )
        {
            data.storeRelationships( result.getAllDiscoveredRelationships() );
        }

        return result;
    }

    public boolean sawDiscovery( final ProjectVersionRef ref )
    {
        return seen.contains( ref );
    }

}
