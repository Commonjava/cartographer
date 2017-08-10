package org.commonjava.cartographer.spi.route;

import org.apache.camel.model.ChoiceDefinition;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.core.structure.RoutingHeaders;
import org.commonjava.cartographer.spi.service.NodeTraverser;
import org.commonjava.propulsor.deploy.camel.route.RouteProvider;

import static org.commonjava.cartographer.core.structure.RouteIds.TRAVERSE;
import static org.commonjava.cartographer.core.structure.Routes.ROUTE_PHASE_CHECK;
import static org.commonjava.cartographer.core.structure.Routes.ROUTE_RECORD_NODE_RESULTS;
import static org.commonjava.cartographer.core.structure.Routes.ROUTE_TRAVERSE_NODE;

/**
 * This base class provides the basic wiring for constructing a route to traverse nodes after they have been resolved.
 * Subclasses must provide a {@link NodeTraverser} instance (normally CDI injected) for use as the processor in the route.
 */
public abstract class TraverserRoute
        extends RouteProvider
{

    @Override
    protected final void configure()
    {
        ChoiceDefinition choice = route().routeId( TRAVERSE.name() )
                                         .from( lookupEndpoint( ROUTE_TRAVERSE_NODE ) )
                                         .bean( getNodeTraverser() )
                                         .choice();

        choice.when( header( MessageHeaders.TRAVERSAL_RESULT ).isEqualTo( MessageHeaders.TraversalResult.TRAVERSAL_DONE ) )
              .to( lookupEndpoint( ROUTE_PHASE_CHECK ) );
    }

    protected abstract NodeTraverser getNodeTraverser();
}
