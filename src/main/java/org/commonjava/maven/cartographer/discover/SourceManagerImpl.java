/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named( "default-carto-source-mgr" )
public class SourceManagerImpl
    implements DiscoverySourceManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public URI createSourceURI( final String source )
    {
        try
        {
            return new URL( source ).toURI();
        }
        catch ( final URISyntaxException e )
        {
            logger.error( "Invalid source URI: {}. Reason: {}", e, source, e.getMessage() );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "Invalid source URL: {}. Reason: {}", e, source, e.getMessage() );
        }

        return null;
    }

    @Override
    public boolean activateWorkspaceSources( final GraphWorkspace ws, final String... sources )
        throws CartoDataException
    {
        boolean result = false;
        for ( final String source : sources )
        {
            final URI src = createSourceURI( source );
            if ( src != null )
            {
                if ( ws.getActiveSources()
                       .contains( src ) )
                {
                    continue;
                }

                ws.addActiveSource( src );
                result = result || ws.getActiveSources()
                                     .contains( src );
            }
        }

        return result;
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
        final List<SimpleLocation> locations = new ArrayList<SimpleLocation>( sources.length );
        for ( final Object source : sources )
        {
            locations.add( new SimpleLocation( source.toString() ) );
        }

        return locations;
    }

    @Override
    public List<? extends Location> createLocations( final Collection<Object> sources )
    {
        final List<SimpleLocation> locations = new ArrayList<SimpleLocation>( sources.size() );
        for ( final Object source : sources )
        {
            locations.add( new SimpleLocation( source.toString() ) );
        }

        return locations;
    }

}
