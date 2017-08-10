package org.commonjava.cartographer.spi.service;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartographer.core.data.dto.ResolutionResult;
import org.commonjava.cartographer.core.data.dto.TraversalResult;
import org.commonjava.cartographer.core.data.work.WorkId;

import java.util.Map;

/**
 * Generates a list of next-hop nodes to resolve/traverse. This is effectively the traversal result for the given
 * graph node, and handles the logic required to filter out relationships that don't match the requested traversal
 * scope.
 *
 * After arriving at a list of target relationships to traverse in the next BSP phase, this processor will set that
 * list in the {@link org.commonjava.cartographer.core.data.work.WorkItem} associated with the {@link WorkId}
 * parameter passed in.
 *
 * This processor will respond with a routing header
 * {@link org.commonjava.cartographer.core.structure.MessageHeaders#TRAVERSAL_RESULT} and a value from
 * {@link org.commonjava.cartographer.core.structure.MessageHeaders.TraversalResult} as follows:
 *
 * <ul>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.TraversalResult#TRAVERSAL_DONE}: This
 *          is always returned currently. The routing header is a placeholder for more advanced status later.</li>
 * </ul>
 */
public interface NodeTraverser
{
    @Handler
    void traverseNode( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
            throws Exception;
}
