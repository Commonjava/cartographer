package org.commonjava.cartographer.spi.service;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartographer.core.data.work.WorkId;

import java.util.List;
import java.util.Map;

/**
 * Each time a {@link org.commonjava.cartographer.core.data.work.WorkItem} exits processing, check whether the request
 * BSP phase is done, and whether another phase is needed. This could result in one of three responses. These responses
 * will include a header, keyed as {@link org.commonjava.cartographer.core.structure.MessageHeaders#BSP_PHASE_CONTROL},
 * with a value from {@link org.commonjava.cartographer.core.structure.MessageHeaders.BspPhaseControl} as follows:
 *
 * <ul>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.BspPhaseControl#START_NEXT_PHASE}:
 *          Return the list of {@link WorkId}'s corresponding to the work ready for the next BSP phase.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.BspPhaseControl#CURRENT_PHASE_IN_PROGRESS}:
 *          The current BSP phase isn't finished; set the header and return a null result here.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.BspPhaseControl#BSP_DONE}:
 *          The current BSP phase is complete, and there is no next phase to process. Return null result here.</li>
 * </ul>
 *
 * If the {@link org.commonjava.cartographer.core.structure.MessageHeaders.BspPhaseControl#BSP_DONE} value is set,
 * another header {@link org.commonjava.cartographer.core.structure.MessageHeaders#BSP_DONE_REASON} should be set with
 * a value from {@link org.commonjava.cartographer.core.structure.MessageHeaders.BspDoneReason}, as follows:
 *
 * <ul>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.BspDoneReason#TRAVERSE_COMPLETE}:
 *          No further nodes have been identified for traversal.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.BspDoneReason#DEPTH_LIMIT_REACHED}:
 *          The max depth set in the user request has been reached.</li>
 * </ul>
 */
public interface BSPBoundaryProcessor
{
    @Handler
    List<WorkId> startNextPhase( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
            throws Exception;
}
