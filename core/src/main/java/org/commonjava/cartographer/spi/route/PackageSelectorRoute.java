package org.commonjava.cartographer.spi.route;

import org.apache.camel.model.ChoiceDefinition;
import org.commonjava.cartographer.core.proc.NodePreResolver;
import org.commonjava.cartographer.core.proc.NodeSelectionRegistrar;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;
import org.commonjava.cartographer.spi.service.NodeSelector;
import org.commonjava.propulsor.deploy.camel.route.RouteProvider;

import javax.inject.Inject;

import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_RESOLVE_NODE_LOOKUP;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_SELECTOR_ERROR;

/**
 * This base class provides the basic wiring for constructing a route to select nodes to resolve during traverse, for a
 * particular package type.
 * Subclasses must provide the package-specific info via {@link CartoPackageInfo}, and a {@link NodeSelector}
 * instance (normally CDI injected) for use as the processor in the route.
 */
// FIXME: How do we need the selection registrar to manage selection errors? We probably want to head off new attempts.
public abstract class PackageSelectorRoute
        extends RouteProvider
{
    @Inject
    private NodeSelectionRegistrar nodeSelectionRegistrar;

    @Inject
    private NodePreResolver preResolver;

    @Override
    protected final void configure()
    {
        CartoPackageInfo pkgInfo = getPackageInfo();

        // NOTE: We're returning back out to the abstracted route that uses the Recipient-List EIP instead of the one below.
        // This is so we can take advantage of a single WireTap EIP on that route to log progress.
        ChoiceDefinition choice =
                route().from( lookupEndpoint( pkgInfo.getSelectorRoute() ) ).bean( getNodeSelector() ).choice();

        choice.when( header( MessageHeaders.SELECTION_RESULT ).in( MessageHeaders.SelectionResult.DONE,
                                                                   MessageHeaders.SelectionResult.AVOIDED ) )
              .bean( nodeSelectionRegistrar )
              .to( lookupEndpoint( ROUTE_RESOLVE_NODE_LOOKUP ) )
              .endChoice()
              .when( header( MessageHeaders.SELECTION_RESULT ).isEqualTo(
                      MessageHeaders.SelectionResult.FAILED ) )
              .to( lookupEndpoint( ROUTE_SELECTOR_ERROR ) )
              .endChoice();
    }

    protected abstract CartoPackageInfo getPackageInfo();

    protected abstract NodeSelector getNodeSelector();
}
