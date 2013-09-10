package org.commonjava.maven.cartographer.discover.patch;

import java.util.Map;

import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.model.Transfer;

public class DistributionProjectPatcher
    implements DepgraphPatcher
{

    @Override
    public DiscoveryResult patch( final DiscoveryResult result, final Transfer transfer, final Map<String, Object> context )
        throws CartoDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getId()
    {
        return "dist";
    }

}
