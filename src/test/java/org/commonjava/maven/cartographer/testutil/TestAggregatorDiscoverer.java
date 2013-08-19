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
import org.commonjava.util.logging.Logger;

public class TestAggregatorDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = new Logger( getClass() );

    private final Map<ProjectVersionRef, DiscoveryResult> mappedResults = new HashMap<>();

    private final Set<ProjectVersionRef> seen = new HashSet<>();

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

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        seen.add( ref );

        final DiscoveryResult result = mappedResults.get( ref );
        logger.info( "DISCOVER: %s....\n  %s", ref, result );
        if ( result != null )
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
