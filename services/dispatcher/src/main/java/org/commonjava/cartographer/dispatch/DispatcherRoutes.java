package org.commonjava.cartographer.dispatch;

import org.commonjava.cartographer.dispatch.route.NodeResolverRecipientList;
import org.commonjava.cartographer.dispatch.route.NodeSelectorRecipientList;
import org.commonjava.propulsor.deploy.camel.route.RouteProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.cartographer.core.structure.RouteIds.RESOLVE;
import static org.commonjava.cartographer.core.structure.RouteIds.SELECT;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_RESOLVE_NODE_LOOKUP;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_SELECT_NODE_LOOKUP;

/**
 * This route collection is concerned with dispatching selection and resolution requests from generic routes to
 * package-specific routes. The processing node that deploys this route will not need to do a lot of heavy computation
 * or data access. It will simply use components that implement the RecipientList EIP to lookup package-specific routes
 * using something like an Infinispan cache as a registry of available package handlers.
 */
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
               .bean( selectorRecipientList );

        route().routeId( RESOLVE.name() )
               .from( lookupEndpoint( ROUTE_RESOLVE_NODE_LOOKUP ) )
               .bean( resolverRecipientList );
    }
}
