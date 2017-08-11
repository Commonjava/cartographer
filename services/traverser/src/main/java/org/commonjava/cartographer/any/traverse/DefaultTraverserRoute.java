package org.commonjava.cartographer.any.traverse;

import org.commonjava.cartographer.spi.route.TraverserRoute;
import org.commonjava.cartographer.spi.service.NodeTraverser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by jdcasey on 8/8/17.
 */
@ApplicationScoped
public class DefaultTraverserRoute
        extends TraverserRoute
{
    @Inject
    private NodeTraverser traverser;

    @Override
    protected NodeTraverser getNodeTraverser()
    {
        return traverser;
    }
}
