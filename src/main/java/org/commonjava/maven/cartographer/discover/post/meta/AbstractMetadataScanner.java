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
package org.commonjava.maven.cartographer.discover.post.meta;

import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMetadataScanner
    implements MetadataScanner
{

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MavenPomReader pomReader;

    protected AbstractMetadataScanner()
    {
    }

    protected AbstractMetadataScanner( final MavenPomReader pomReader )
    {
        this.pomReader = pomReader;
    }

    @Override
    public Map<String, String> scan( final ProjectVersionRef ref, final List<? extends Location> locations, final Map<String, Object> context )
    {
        MavenPomView pomView = (MavenPomView) context.get( POM_VIEW_CTX_KEY );
        if ( pomView == null )
        {
            try
            {
                final Transfer transfer = (Transfer) context.get( TRANSFER_CTX_KEY );
                if ( transfer != null )
                {
                    pomView = pomReader.read( ref, transfer, locations );
                }
                else
                {
                    pomView = pomReader.read( ref, locations );
                }

                context.put( POM_VIEW_CTX_KEY, pomView );
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( "Failed to parse: {}. Reason: {}", e, ref, e.getMessage() );
                return null;
            }
        }

        return scan( ref, pomView );
    }

    protected Map<String, String> scan( final ProjectVersionRef ref, final MavenPomView pomView )
    {
        final Map<String, String> xpaths = getMetadataKeyXPathMappings();
        final Map<String, String> metadata = new HashMap<String, String>( xpaths.size() );
        for ( final Map.Entry<String, String> entry : xpaths.entrySet() )
        {
            final String key = entry.getKey();
            final String xpath = entry.getValue();

            try
            {
                final String scmConnection = pomView.resolveXPathExpression( xpath, false );
                if ( scmConnection != null )
                {
                    metadata.put( key, scmConnection );
                }
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( "Failed to resolve SCM element via XPath '{}' in: {}. Reason: {}", e, xpath, ref, e.getMessage() );
            }
        }

        return metadata;
    }

    protected Map<String, String> getMetadataKeyXPathMappings()
    {
        return Collections.emptyMap();
    }

}
