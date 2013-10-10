package org.commonjava.maven.cartographer.discover.patch;

import java.util.List;
import java.util.Map;

import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.model.Location;

public interface DepgraphPatcher
{

    void patch( DiscoveryResult result, List<? extends Location> locations, Map<String, Object> context );

    String getId();

}
