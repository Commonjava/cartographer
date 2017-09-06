package org.commonjava.cartographer.spi.route;

import org.apache.camel.model.ChoiceDefinition;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;
import org.commonjava.cartographer.spi.service.NodeResolver;
import org.commonjava.propulsor.deploy.camel.route.RouteProvider;

import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_RESOLVER_ERROR;
import static org.commonjava.cartographer.core.structure.EndpointKeys.ROUTE_TRAVERSE_NODE;

/**
 * This base class provides the basic wiring for constructing a route to resolve nodes for a particular package type.
 * Subclasses must provide the package-specific info via {@link CartoPackageInfo}, and a {@link NodeResolver}
 * instance (normally CDI injected) for use as the processor in the route.
 */
public abstract class PackageResolverRoute
        extends RouteProvider
{
    @Override
    protected final void configure()
    {
        CartoPackageInfo pkgInfo = getPackageInfo();

        // NOTE: We're returning back out to the abstracted route that uses the Recipient-List EIP instead of the one below.
        // This is so we can take advantage of a single WireTap EIP on that route to log progress.
        ChoiceDefinition choice =
                route().from( lookupEndpoint( pkgInfo.getResolverRoute() ) ).bean( getNodeResolver() ).choice();


        choice.when( header( MessageHeaders.RESOLUTION_RESULT ).isEqualTo( MessageHeaders.ResolutionResult.DONE ) )
               .to( lookupEndpoint( ROUTE_TRAVERSE_NODE ) );

        choice.when( header( MessageHeaders.RESOLUTION_RESULT ).in( MessageHeaders.ResolutionResult.ERROR,
                                                                    MessageHeaders.ResolutionResult.FAILED ) )
              .to( lookupEndpoint( ROUTE_RESOLVER_ERROR ) );
    }

    protected abstract CartoPackageInfo getPackageInfo();

    protected abstract NodeResolver getNodeResolver();
}
