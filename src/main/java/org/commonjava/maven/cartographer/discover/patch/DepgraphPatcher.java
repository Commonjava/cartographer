package org.commonjava.maven.cartographer.discover.patch;

import java.util.Map;

import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.model.Transfer;

public interface DepgraphPatcher
{

    String MAVEN_MODEL_CTX_KEY = "maven-model";

    DiscoveryResult patch( DiscoveryResult result, Transfer transfer, Map<String, Object> context )
        throws CartoDataException;

    String getId();

}
