/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.discover.post.patch;

import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
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

    public DiscoveryResult patch( final DiscoveryResult orig, final Set<String> enabledPatchers, final List<? extends Location> locations,
                                  final MavenPomView pomView, final Transfer transfer )
    {
        if ( enabledPatchers == null || enabledPatchers.isEmpty() )
        {
            return orig;
        }

        logger.debug( "Running enabled patchers: {} (available patchers: {})", new JoinString( ", ", patchers.keySet() ),
                      new JoinString( ", ", enabledPatchers ) );
        final DiscoveryResult result = orig;
        final Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put( POM_VIEW_CTX_KEY, pomView );
        ctx.put( TRANSFER_CTX_KEY, transfer );

        final Set<String> done = new HashSet<String>();
        for ( final String patcherId : enabledPatchers )
        {
            if ( done.contains( patcherId ) )
            {
                continue;
            }

            final DepgraphPatcher patcher = patchers.get( patcherId );
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
                logger.error( String.format( "Failed to execute patcher: %s against: %s. Reason: %s", patcherId, result, e.getMessage() ), e );
            }

            logger.debug( "After patching with {}, result is: {}", patcherId, result );

            done.add( patcherId );
        }

        return result;
    }

}
