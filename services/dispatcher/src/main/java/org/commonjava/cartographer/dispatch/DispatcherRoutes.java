package org.commonjava.cartographer.dispatch;

import org.commonjava.cartographer.core.proc.NodePreResolver;
import org.commonjava.cartographer.core.proc.NodePreSelector;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.dispatch.route.NodeResolverRecipientList;
import org.commonjava.cartographer.dispatch.route.NodeSelectorRecipientList;
import org.commonjava.propulsor.deploy.camel.route.RouteProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_RESOLVE_NODE_LOOKUP;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_SELECT_NODE_LOOKUP;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_TRAVERSE_NODE;
import static org.commonjava.cartographer.core.structure.RouteIds.RESOLVE;
import static org.commonjava.cartographer.core.structure.RouteIds.SELECT;

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

    @Inject
    private NodeSelectorRecipientList selectorRecipientList;

    @Inject
    private NodePreResolver preResolver;

    @Inject
    private NodePreSelector preSelector;

    protected void configure()
    {
        route().routeId( SELECT.name() )
               .from( lookupEndpoint( ROUTE_SELECT_NODE_LOOKUP ) )
               .bean( preSelector )
               .choice()
               .when( header( MessageHeaders.SELECT_STATUS ).isEqualTo( MessageHeaders.SelectStatus.SELECTING ) )
               .to( MessageHeaders.ROUTE_WAIT_SELECTOR )
               .endChoice()
               .when( header( MessageHeaders.SELECT_STATUS ).isEqualTo( MessageHeaders.SelectStatus.DONE ) )
               .to( ROUTE_RESOLVE_NODE_LOOKUP )
               .endChoice()
               .otherwise()
               .bean( selectorRecipientList );

        route().routeId( RESOLVE.name() )
               .from( lookupEndpoint( ROUTE_RESOLVE_NODE_LOOKUP ) )
               .bean( preResolver )
               .choice()
               .when( header( MessageHeaders.RESOLVE_STATUS ).isEqualTo(
                       MessageHeaders.ResolutionState.RESOLVING ) )
               .to( MessageHeaders.ROUTE_WAIT_RESOLVER )
               .endChoice()
               .when( header( MessageHeaders.RESOLVE_STATUS ).isEqualTo(
                       MessageHeaders.ResolutionState.DONE ) )
               .to( ROUTE_TRAVERSE_NODE )
               .endChoice()
               .otherwise()
               .bean( resolverRecipientList );
    }
}
