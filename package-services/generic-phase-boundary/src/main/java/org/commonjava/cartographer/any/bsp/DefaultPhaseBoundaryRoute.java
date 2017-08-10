package org.commonjava.cartographer.any.bsp;

import org.commonjava.cartographer.spi.route.PhaseBoundaryRoute;
import org.commonjava.cartographer.spi.service.BSPBoundaryProcessor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @see org.commonjava.cartographer.spi.route.PhaseBoundaryRoute
 */
@ApplicationScoped
public class DefaultPhaseBoundaryRoute
        extends PhaseBoundaryRoute
{
    @Inject
    private BSPBoundaryProcessor boundaryProcessor;

    @Override
    protected BSPBoundaryProcessor getBoundaryProcessor()
    {
        return boundaryProcessor;
    }
}
