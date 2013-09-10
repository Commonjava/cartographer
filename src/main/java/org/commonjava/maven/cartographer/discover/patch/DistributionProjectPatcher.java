package org.commonjava.maven.cartographer.discover.patch;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.reader.MavenPomReader;
import org.commonjava.maven.galley.maven.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.util.logging.Logger;

public class DistributionProjectPatcher
    implements DepgraphPatcher
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private MavenPomReader pomReader;

    @Override
    public DiscoveryResult patch( final DiscoveryResult orig, final List<? extends Location> locations, final Map<String, Object> context )
    {
        final DiscoveryResult result = orig;
        try
        {
            final MavenPomView pomView = pomReader.read( result.getSelectedRef(), locations );
            // TODO: check for dependency:unpack, verify all deps are available as relationships and add what aren't
            // TODO: find a way to detect an assembly/distro pom, and turn deps from provided scope to compile scope.
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Failed to build MavenPomView for: %s from: %s. Reason: %s", e, result.getSelectedRef(), locations, e.getMessage() );
        }

        return result;
    }

    @Override
    public String getId()
    {
        return "dist";
    }

}
