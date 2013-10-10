package org.commonjava.maven.cartographer.discover.patch;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

public class PatcherSupport
{

    private final Logger logger = new Logger( getClass() );

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

    private void mapPatchers( final Iterable<DepgraphPatcher> patcherInstances )
    {
        this.patchers = new HashMap<>();
        for ( final DepgraphPatcher patcher : patcherInstances )
        {
            patchers.put( patcher.getId(), patcher );
        }
    }

    public DiscoveryResult patch( final DiscoveryResult orig, final Set<String> enabledPatchers, final List<? extends Location> locations,
                                  final MavenPomView pomView, final Transfer transfer )
    {
        logger.info( "Running enabled patchers: %s (available patchers: %s)", join( enabledPatchers, ", " ), join( patchers.keySet(), ", " ) );
        final DiscoveryResult result = orig;
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put( POM_VIEW_CTX_KEY, pomView );
        ctx.put( TRANSFER_CTX_KEY, transfer );

        for ( final String patcherId : enabledPatchers )
        {
            final DepgraphPatcher patcher = patchers.get( patcherId );
            if ( patcher == null )
            {
                logger.warn( "No such dependency-graph patcher: '%s'", patcherId );
                continue;
            }

            logger.info( "Running project-relationship patcher: %s for: %s", patcherId, orig.getSelectedRef() );
            try
            {
                patcher.patch( result, locations, ctx );
            }
            catch ( final Exception e )
            {
                logger.error( "Failed to execute patcher: %s against: %s. Reason: %s", e, patcherId, result, e.getMessage() );
            }

            logger.info( "After patching with %s, result is: %s", patcherId, result );
        }

        return result;
    }

}
