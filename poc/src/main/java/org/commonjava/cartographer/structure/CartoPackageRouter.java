package org.commonjava.cartographer.structure;

import org.commonjava.cartographer.data.global.CartoPackageInfo;
import org.commonjava.cartographer.proc.resolve.CartoNodeResolver;
import org.commonjava.cartographer.proc.select.CartoNodeSelector;

import static org.commonjava.cartographer.structure.RouteIds.SELECT;
import static org.commonjava.cartographer.structure.Routes.ROUTE_RESOLVER_ERROR;
import static org.commonjava.cartographer.structure.Routes.ROUTE_TRAVERSE_NODE;
import static org.commonjava.cartographer.structure.RoutingHeaders.TRAVERSE_NODE;
import static org.commonjava.cartographer.structure.RoutingHeaders.RESOLVE_ERROR;

/**
 * Created by jdcasey on 7/10/17.
 */
public abstract class CartoPackageRouter
        extends CartoRouteBuilder
{
    @Override
    protected final void configure()
    {
        CartoPackageInfo pkgInfo = getPackageInfo();

        // NOTE: We're returning back out to the abstracted route that uses the Recipient-List EIP instead of the one below.
        // This is so we can take advantage of a single WireTap EIP on that route to log progress.
        route().from( pkgInfo.getSelectorRoute() ).bean( getNodeSelector() ).to( Routes.ROUTE_SELECT_NODE_LOOKUP );

        route().from( pkgInfo.getResolverRoute() )
               .bean( getNodeResolver() )
               .choice()
               .when( header( TRAVERSE_NODE.name() ) )
               .to( ROUTE_TRAVERSE_NODE )
               .when( header( RESOLVE_ERROR.name() ) )
               .to( ROUTE_RESOLVER_ERROR );
    }

    protected abstract CartoPackageInfo getPackageInfo();

    protected abstract Class<? extends CartoNodeResolver> getNodeResolver();

    protected abstract Class<? extends CartoNodeSelector> getNodeSelector();
}
