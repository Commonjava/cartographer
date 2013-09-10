package org.commonjava.maven.cartographer.discover.patch;

import java.util.List;
import java.util.Map;

import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.model.Location;

public interface DepgraphPatcher
{

    String MAVEN_MODEL_CTX_KEY = "maven-model";

    String TRANSFER_CTX_KEY = "transfer";

    DiscoveryResult patch( DiscoveryResult result, List<? extends Location> locations, Map<String, Object> context );

    String getId();

}
