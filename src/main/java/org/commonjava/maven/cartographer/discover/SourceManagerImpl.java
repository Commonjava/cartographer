package org.commonjava.maven.cartographer.discover;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
@Named( "default-carto-source-mgr" )
public class SourceManagerImpl
    implements DiscoverySourceManager
{

    private final Logger logger = new Logger( getClass() );

    @Override
    public URI createSourceURI( final String source )
    {
        try
        {
            return new URL( source ).toURI();
        }
        catch ( final URISyntaxException e )
        {
            logger.error( "Invalid source URI: %s. Reason: %s", e, source, e.getMessage() );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "Invalid source URL: %s. Reason: %s", e, source, e.getMessage() );
        }

        return null;
    }

    @Override
    public void activateWorkspaceSources( final GraphWorkspace ws, final String... sources )
        throws CartoDataException
    {
        for ( final String source : sources )
        {
            final URI src = createSourceURI( source );
            if ( src != null )
            {
                ws.addActiveSource( src );
            }
        }
    }

    @Override
    public String getFormatHint()
    {
        return "Any valid URL supported by a configured galley transport";
    }

    @Override
    public Location createLocation( final Object source )
    {
        return new SimpleLocation( source.toString() );
    }

    @Override
    public List<? extends Location> createLocations( final Object... sources )
    {
        final List<SimpleLocation> locations = new ArrayList<>( sources.length );
        for ( final Object source : sources )
        {
            locations.add( new SimpleLocation( source.toString() ) );
        }

        return locations;
    }

    @Override
    public List<? extends Location> createLocations( final Collection<Object> sources )
    {
        final List<SimpleLocation> locations = new ArrayList<>( sources.size() );
        for ( final Object source : sources )
        {
            locations.add( new SimpleLocation( source.toString() ) );
        }

        return locations;
    }

}
