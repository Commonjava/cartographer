package org.commonjava.maven.cartographer.discover.patch;

import static org.apache.commons.lang.StringUtils.join;

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
        DiscoveryResult result = orig;
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put( DepgraphPatcher.POM_VIEW, pomView );
        ctx.put( DepgraphPatcher.TRANSFER_CTX_KEY, transfer );

        for ( final String patcherId : enabledPatchers )
        {
            final DepgraphPatcher patcher = patchers.get( patcherId );
            if ( patcher == null )
            {
                logger.warn( "No such dependency-graph patcher: '%s'", patcherId );
                continue;
            }

            logger.info( "Running project-relationship patcher: %s for: %s", patcherId, orig.getSelectedRef() );
            result = patcher.patch( result, locations, ctx );
        }

        return result;
    }

}
