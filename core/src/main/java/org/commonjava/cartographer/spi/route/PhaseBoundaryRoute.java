package org.commonjava.cartographer.spi.route;

import org.apache.camel.model.ChoiceDefinition;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.spi.service.BSPBoundaryProcessor;
import org.commonjava.cartographer.spi.service.NodeTraverser;
import org.commonjava.propulsor.deploy.camel.route.RouteProvider;

import static org.commonjava.cartographer.core.structure.RouteIds.BSP_BOUNDARY;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_END_REQUEST;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_PHASE_BOUNDARY;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_SELECT_NODE_LOOKUP;

/**
 * This base class provides the basic wiring for constructing a route to traverse nodes after they have been resolved.
 * Subclasses must provide a {@link NodeTraverser} instance (normally CDI injected) for use as the processor in the route.
 */
public abstract class PhaseBoundaryRoute
        extends RouteProvider
{

    @Override
    protected final void configure()
    {
        ChoiceDefinition choice = route().routeId( BSP_BOUNDARY.name() )
                                         .from( lookupEndpoint( ROUTE_PHASE_BOUNDARY ) )
                                         .bean( getBoundaryProcessor() )
                                         .choice();

        choice.when( header( MessageHeaders.BSP_PHASE_CONTROL ).isEqualTo( MessageHeaders.BspPhaseControl.BSP_DONE ) )
              .to( lookupEndpoint( ROUTE_END_REQUEST ) );

        choice.when( header( MessageHeaders.BSP_PHASE_CONTROL ).isEqualTo(
                MessageHeaders.BspPhaseControl.START_NEXT_PHASE ) )
              .split()
              .body()
              .to( lookupEndpoint( ROUTE_SELECT_NODE_LOOKUP ) );
    }

    protected abstract BSPBoundaryProcessor getBoundaryProcessor();
}
