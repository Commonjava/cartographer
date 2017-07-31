package org.commonjava.cartographer.structure;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.BuilderSupport;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;

/**
 * Created by jdcasey on 7/10/17.
 */
public abstract class CartoRouteBuilder
        extends BuilderSupport
        implements RoutesBuilder
{
    private final RoutesDefinition routes = new RoutesDefinition();

    @Override
    public final void addRoutesToCamelContext( final CamelContext context )
            throws Exception
    {
        for ( RouteDefinition route : routes.getRoutes() )
        {
            context.addRouteDefinition( route );
        }
    }

    protected abstract void configure();

    protected final RouteDefinition route()
    {
        return routes.route();
    }
}
