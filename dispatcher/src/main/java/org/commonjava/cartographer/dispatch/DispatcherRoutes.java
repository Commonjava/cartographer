package org.commonjava.cartographer.dispatch;

import org.commonjava.cartographer.dispatch.route.NodeResolverRecipientList;
import org.commonjava.cartographer.dispatch.route.NodeSelectorRecipientList;
import org.commonjava.propulsor.deploy.camel.route.RouteProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.cartographer.core.structure.RouteIds.RESOLVE;
import static org.commonjava.cartographer.core.structure.RouteIds.SELECT;
import static org.commonjava.cartographer.core.structure.Routes.ROUTE_PROGRESS_NODE_ADDED;
import static org.commonjava.cartographer.core.structure.Routes.ROUTE_RESOLVE_NODE_LOOKUP;
import static org.commonjava.cartographer.core.structure.Routes.ROUTE_SELECT_NODE_LOOKUP;

@ApplicationScoped
public class DispatcherRoutes
        extends RouteProvider
{

    @Inject
    private NodeResolverRecipientList resolverRecipientList;

    private NodeSelectorRecipientList selectorRecipientList;

    protected void configure()
    {


        route().routeId( SELECT.name() )
               .from( lookupEndpoint( ROUTE_SELECT_NODE_LOOKUP ) )
               .wireTap( ROUTE_PROGRESS_NODE_ADDED )
               .bean( selectorRecipientList );

        route().routeId( RESOLVE.name() )
               .from( lookupEndpoint( ROUTE_RESOLVE_NODE_LOOKUP ) )
               .wireTap( ROUTE_PROGRESS_NODE_ADDED )
               .bean( resolverRecipientList );

//        route().routeId( PROGRESS.name() )
//               .from( ROUTE_PROGRESS_NODE_ADDED, ROUTE_PROGRESS_NODE_TRAVERSED, ROUTE_PROGRESS_NODE_RESOLVED )
//               .bean( RequestProgressCorrelator.class );
//
//        route().routeId( ERRORS.name() ).from( ROUTE_NODE_CYCLE_ERROR, ROUTE_RESOLVER_ERROR ).bean( ErrorCorrelator.class );
    }
}
