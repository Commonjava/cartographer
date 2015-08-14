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
package org.commonjava.cartographer.graph.discover.meta;

import static org.commonjava.cartographer.INTERNAL.graph.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.cartographer.INTERNAL.graph.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.cartographer.spi.graph.discover.meta.MetadataScanner;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataScannerSupport
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<MetadataScanner> scannerInstances;

    private Iterable<MetadataScanner> scanners;

    protected MetadataScannerSupport()
    {
    }

    public MetadataScannerSupport( final MetadataScanner... scanners )
    {
        this.scanners = new HashSet<MetadataScanner>( Arrays.asList( scanners ) );
    }

    public MetadataScannerSupport( final Collection<MetadataScanner> scanners )
    {
        this.scanners = new HashSet<MetadataScanner>( scanners );
    }

    @PostConstruct
    public void setScanners()
    {
        this.scanners = this.scannerInstances;
    }

    public Map<String, String> scan( final ProjectVersionRef ref, final List<? extends Location> locations,
                                     final MavenPomView pomView, final Transfer transfer )
    {
        if ( scanners == null || !scanners.iterator()
                                          .hasNext() )
        {
            return null;
        }

        final Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put( POM_VIEW_CTX_KEY, pomView );
        ctx.put( TRANSFER_CTX_KEY, transfer );

        final Map<String, String> result = new HashMap<String, String>();
        for ( final MetadataScanner scanner : scanners )
        {
            if ( scanner == null )
            {
                continue;
            }

            logger.debug( "Running metadata scanner: {} for: {}", scanner.getClass()
                                                                         .getSimpleName(), ref );
            try
            {
                final Map<String, String> scanResult = scanner.scan( ref, locations, ctx );
                if ( scanResult != null && !scanResult.isEmpty() )
                {
                    result.putAll( scanResult );
                }
            }
            catch ( final Exception e )
            {
                logger.error( "Failed to execute metadata scanner: {} against: {}. Reason: {}", e,
                              scanner.getClass()
                                     .getSimpleName(), ref, e.getMessage() );
            }
        }

        return result;
    }

}
