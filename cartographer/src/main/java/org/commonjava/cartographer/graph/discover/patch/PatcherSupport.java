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
package org.commonjava.cartographer.graph.discover.patch;

import static org.commonjava.cartographer.INTERNAL.graph.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.cartographer.INTERNAL.graph.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatcherSupport
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<DepgraphPatcher> patcherInstances;

    private Map<String, DepgraphPatcher> patchers;

    protected PatcherSupport()
    {
    }

    public PatcherSupport( final DepgraphPatcher... patchers )
    {
        mapPatchers( Arrays.asList( patchers ) );
    }

    @PostConstruct
    public void mapPatchers()
    {
        mapPatchers( this.patcherInstances );
    }

    public Set<String> getAvailablePatchers()
    {
        return new HashSet<String>( patchers.keySet() );
    }

    private void mapPatchers( final Iterable<DepgraphPatcher> patcherInstances )
    {
        this.patchers = new HashMap<String, DepgraphPatcher>();
        for ( final DepgraphPatcher patcher : patcherInstances )
        {
            patchers.put( patcher.getId(), patcher );
        }
    }

    public DiscoveryResult patch( final DiscoveryResult orig, final Collection<String> patchers,
                                  final List<? extends Location> locations, final MavenPomView pomView,
                                  final Transfer transfer )
    {
        if ( patchers == null || patchers.isEmpty() )
        {
            return orig;
        }

        Set<String> patcherIds = new HashSet<>( patchers );
        if ( patcherIds.contains(DepgraphPatcherConstants.ALL))
        {
            patcherIds.addAll(this.patchers.keySet());
        }

        logger.debug( "Running enabled patchers: {} (available patchers: {})",
                      new JoinString( ", ", this.patchers.keySet() ), new JoinString( ", ", patcherIds ) );

        final DiscoveryResult result = orig;
        final Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put( POM_VIEW_CTX_KEY, pomView );
        ctx.put( TRANSFER_CTX_KEY, transfer );

        final Set<String> done = new HashSet<String>();
        for ( final String patcherId : patcherIds )
        {
            if ( done.contains( patcherId ) )
            {
                continue;
            }

            final DepgraphPatcher patcher = this.patchers.get( patcherId );
            if ( patcher == null )
            {
                logger.warn( "No such dependency-graph patcher: '{}'", patcherId );
                continue;
            }

            logger.info( "Running project-relationship patcher: {} for: {}", patcherId, orig.getSelectedRef() );
            try
            {
                patcher.patch( result, locations, ctx );
            }
            catch ( final Exception e )
            {
                logger.error( String.format( "Failed to execute patcher: %s against: %s. Reason: %s", patcherId,
                                             result, e.getMessage() ), e );
            }

            logger.debug( "After patching with {}, result is: {}", patcherId, result );

            done.add( patcherId );
        }

        return result;
    }

}
