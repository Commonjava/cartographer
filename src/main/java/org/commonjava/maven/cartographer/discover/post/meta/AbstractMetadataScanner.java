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
    public Map<String, String> scan( final ProjectVersionRef ref, final List<? extends Location> locations,
                                     final Map<String, Object> context )
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
                logger.error( String.format( "Failed to parse: %s. Reason: %s", ref, e.getMessage() ), e );
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
                logger.error( String.format( "Failed to resolve SCM element via XPath '%s' in: %s. Reason: %s", xpath,
                                             ref, e.getMessage() ), e );
            }
        }

        return metadata;
    }

    protected Map<String, String> getMetadataKeyXPathMappings()
    {
        return Collections.emptyMap();
    }

}
