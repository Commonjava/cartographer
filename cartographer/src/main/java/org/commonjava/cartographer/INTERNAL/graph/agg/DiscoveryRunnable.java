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
package org.commonjava.cartographer.INTERNAL.graph.agg;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.cartographer.spi.graph.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.cartographer.graph.agg.AggregationOptions;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class DiscoveryRunnable
    implements Runnable
{

    private final AggregationOptions config;

    private CountDownLatch latch;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<ProjectVersionRef> roMissing;

    private final ProjectRelationshipDiscoverer discoverer;

    private final DiscoveryTodo todo;

    private DiscoveryResult result;

    private final int pass;

    private final int idx;

    private Throwable error;

    public DiscoveryRunnable( final DiscoveryTodo todo, final AggregationOptions config,
                              final Set<ProjectVersionRef> missing, final ProjectRelationshipDiscoverer discoverer,
                              final int pass, final int idx )
    {
        this.todo = todo;
        this.config = config;
        this.roMissing = missing;
        this.discoverer = discoverer;
        this.pass = pass;
        this.idx = idx;
    }

    @Override
    public void run()
    {
        final ProjectVersionRef ref = todo.getRef();

        logger.info( "\n\n\n\n{}.{}. Discovering project graph for: {}\n\n\n\n", pass, idx, ref );

        try
        {
            final DiscoveryConfig discoveryConfig = config.getDiscoveryConfig();

            if ( discoverer != null && !roMissing.contains( ref ) )
            {
                result = discoverer.discoverRelationships( ref, todo.getGraph(), discoveryConfig );
            }
            else if ( roMissing.contains( ref ) )
            {
                logger.debug( "{}.{}. MISS: Already marked as missing: {}", pass, idx, ref );
            }
            else
            {
                logger.error( "No discoverer! Skipping: {}", ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( String.format( "{}.%s. Cannot discover subgraph for: %s. Reason: %s.", pass, idx, ref,
                                         e.getMessage() ), e );
            error = e;
        }
        catch ( final CartoDataException e )
        {
            logger.error( String.format( "{}.%s. Failed to discover subgraph for: %s. Reason: %s.", pass, idx, ref,
                                         e.getMessage() ), e );
            error = e;
        }
        finally
        {
            if ( latch != null )
            {
                latch.countDown();
            }
        }
    }

    public Throwable getError()
    {
        return error;
    }

    public int getIndex()
    {
        return idx;
    }

    public DiscoveryResult getResult()
    {
        return result;
    }

    public DiscoveryTodo getTodo()
    {
        return todo;
    }

    public void setLatch( final CountDownLatch latch )
    {
        this.latch = latch;
    }

}
