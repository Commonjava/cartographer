package org.commonjava.maven.cartographer.discover;

import java.net.URI;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;

public interface DiscoverySourceManager
{

    URI createSourceURI( String source );

    void activateWorkspaceSources( GraphWorkspace ws, String... sources )
        throws CartoDataException;

    String getFormatHint();

}
