/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.INTERNAL.graph.discover;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Resource;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.VirtualResource;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.spi.transport.LocationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Named
public class SourceManagerImpl
    implements DiscoverySourceManager, LocationExpander, LocationResolver
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<String, String> aliases = new HashMap<String, String>();

    /**
     * Alias the given URL with a new short-handed key for use in various cartographer recipes.
     * 
     * @param alias
     * @param url
     */
    public SourceManagerImpl withAlias( final String alias, final String url )
    {
        aliases.put( alias, url );
        return this;
    }

    /**
     * Alias the given URL with a new short-handed key for use in various cartographer recipes.
     *
     * @param alias
     * @param url
     */
    public boolean addSourceAlias( final String alias, final String url )
    {
        if ( aliases.containsKey( alias ) )
        {
            return false;
        }

        aliases.put( alias, url );
        return true;
    }

    /**
     * Add the aliases given in the {@link Map} object to the current mapping.
     * 
     * @param aliases
     */
    public SourceManagerImpl withAliases( final Map<String, String> aliases )
    {
        this.aliases.putAll( aliases );

        return this;
    }

    /**
     * Add the aliases given in the {@link Properties} object to the current mapping.
     * 
     * @param aliases
     */
    public SourceManagerImpl withAliases( final Properties aliases )
    {
        for ( final Enumeration<?> names = aliases.propertyNames(); names.hasMoreElements(); )
        {
            final String name = (String) names.nextElement();
            this.aliases.put( name, aliases.getProperty( name ) );
        }
        return this;
    }

    @Override
    public URI createSourceURI( final String source )
    {
        try
        {
            final String value = aliases.get( source );
            final String u = value == null ? source : value;
            return new URL( u ).toURI();
        }
        catch ( final URISyntaxException e )
        {
            logger.error( String.format( "Invalid source URI: %s. Reason: %s", source, e.getMessage() ), e );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( String.format( "Invalid source URL: %s. Reason: %s", source, e.getMessage() ), e );
        }

        return null;
    }

    @Override
    public boolean activateWorkspaceSources( final ViewParams params, final String... sources )
        throws CartoDataException
    {
        logger.debug( "Original source locations: {}", params.getActiveSources() );
        boolean result = false;
        for ( final String source : sources )
        {
            final URI src = createSourceURI( source );
            if ( src != null )
            {
                if ( params.getActiveSources()
                           .contains( src ) )
                {
                    continue;
                }

                logger.debug( "Adding source location: {}", src );
                params.addActiveSource( src );

                result = result || params.getActiveSources()
                                         .contains( src );
            }
        }

        return result;
    }

    @Override
    public boolean activateWorkspaceSources( final ViewParams params, final Collection<? extends Location> sources )
        throws CartoDataException
    {
        logger.debug( "Original source locations: {}", params.getActiveSources() );
        boolean result = false;
        for ( final Location source : sources )
        {
            final URI src = createSourceURI( source.getUri() );
            if ( src != null )
            {
                if ( params.getActiveSources()
                           .contains( src ) )
                {
                    continue;
                }

                logger.debug( "Adding source location: {}", src );
                params.addActiveSource( src );

                result = result || params.getActiveSources()
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
    public Map<String, String> getAliasMap()
    {
        return Collections.unmodifiableMap( aliases );
    }

    @Override
    public Location createLocation( final Object source )
    {
        final String value = aliases.get( source.toString() );
        return new SimpleLocation( value == null ? source.toString() : value );
    }

    @Override
    public List<? extends Location> createLocations( final Object... sources )
    {
        final List<SimpleLocation> locations = new ArrayList<SimpleLocation>();
        for ( final Object source : sources )
        {
            final String value = aliases.get( source.toString() );
            locations.add( new SimpleLocation( value == null ? source.toString() : value ) );
        }

        return locations;
    }

    @Override
    public List<? extends Location> createLocations( final Collection<Object> sources )
    {
        final List<SimpleLocation> locations = new ArrayList<SimpleLocation>();
        for ( final Object source : sources )
        {
            final String value = aliases.get( source.toString() );
            locations.add( new SimpleLocation( value == null ? source.toString() : value ) );
        }

        return locations;
    }

    @Override
    public List<Location> expand( final Location... locations )
    {
        logger.debug( "Expanding location array: {}", Arrays.toString( locations ) );
        final List<Location> result = new ArrayList<Location>();
        for ( final Location source : locations )
        {
            final String value = aliases.get( source.getUri() );
            if ( value == null )
            {
                result.add( source );
            }
            else
            {
                result.add( new SimpleLocation( value ) );
            }
        }

        logger.debug( "Result: {}", result );

        return result;
    }

    @Override
    public <T extends Location> List<Location> expand( final Collection<T> locations )
        throws TransferException
    {
        logger.debug( "Expanding location collection: {}", locations );
        final List<Location> result = new ArrayList<Location>();
        for ( final Location source : locations )
        {
            final String value = aliases.get( source.getUri() );
            if ( value == null )
            {
                result.add( source );
            }
            else
            {
                result.add( new SimpleLocation( value ) );
            }
        }

        logger.debug( "Result: {}", result );

        return result;
    }

    @Override
    public VirtualResource expand( final Resource resource )
        throws TransferException
    {
        logger.debug( "Expanding virtual: {}", resource );
        final List<ConcreteResource> res = new ArrayList<ConcreteResource>();
        if ( resource instanceof VirtualResource )
        {
            final VirtualResource virtual = (VirtualResource) resource;
            for ( final ConcreteResource concrete : virtual )
            {
                res.addAll( expandConcrete( concrete ) );
            }
        }
        else
        {
            res.addAll( expandConcrete( (ConcreteResource) resource ) );
        }

        final VirtualResource result = new VirtualResource( res );
        logger.debug( "Result: {}", result );
        return result;
    }

    private List<ConcreteResource> expandConcrete( final ConcreteResource concrete )
    {
        final Location source = concrete.getLocation();
        final List<ConcreteResource> result = new ArrayList<ConcreteResource>();
        for ( final Location loc : expand( source ) )
        {
            result.add( new ConcreteResource( loc, concrete.getPath() ) );
        }

        return result;
    }

    @Override
    public Location resolve( final String spec )
        throws TransferException
    {
        return createLocation( spec );
    }

}
