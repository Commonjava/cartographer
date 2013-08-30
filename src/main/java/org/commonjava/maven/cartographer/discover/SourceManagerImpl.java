package org.commonjava.maven.cartographer.discover;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
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

}
