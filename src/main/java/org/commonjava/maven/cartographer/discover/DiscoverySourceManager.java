package org.commonjava.maven.cartographer.discover;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.galley.model.Location;

public interface DiscoverySourceManager
{

    Location createLocation( Object source );

    List<? extends Location> createLocations( Object... sources );

    List<? extends Location> createLocations( Collection<Object> sources );

    URI createSourceURI( String source );

    void activateWorkspaceSources( GraphWorkspace ws, String... sources )
        throws CartoDataException;

    String getFormatHint();

}
