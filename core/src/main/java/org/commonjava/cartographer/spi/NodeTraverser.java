package org.commonjava.cartographer.spi;

import org.apache.camel.Handler;
import org.commonjava.cartographer.core.data.data.global.dto.TraversalRequest;
import org.commonjava.cartographer.core.data.data.global.dto.TraversalResult;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;

/**
 * Generates a list of next-hop nodes to resolve/traverse. This is effectively the traversal result for the given
 * graph node, after filtering out the relationships that don't match.
 */
@ApplicationScoped
public class NodeTraverser
{
    @Handler
    public TraversalResult traverseNode( final TraversalRequest request )
            throws Exception
    {
        // FIXME Implement.
        return new TraversalResult( request, false, Collections.emptyList() );
    }
}
