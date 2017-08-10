package org.commonjava.cartographer.spi.service;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.spi.data.CartoPackageInfo;

import java.util.Map;

/**
 * Component responsible for weighing user-requested version overrides, previous version selections for similar nodes,
 * and the current node request, then selecting an output node coordinate to resolve.
 *
 * This processor will respond with a routing header
 * {@link org.commonjava.cartographer.core.structure.MessageHeaders#SELECTION_RESULT} that has a value from
 * {@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult} as follows:
 *
 * <ul>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#DONE}: Resolved the
 *          {@link org.commonjava.cartgorapher.model.graph.PkgId} to a {@link org.commonjava.cartgorapher.model.graph.PkgVersion},
 *          which is now ready to resolve.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#SELECTION_FAILED}:
 *          Could not find a valid {@link org.commonjava.cartgorapher.model.graph.PkgVersion} selection for the given
 *          {@link org.commonjava.cartgorapher.model.graph.PkgId} that satisfied version constraints.</li>
 * </ul>
 *
 * If {@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#SELECTION_FAILED} is returned,
 * another header called {@link org.commonjava.cartographer.core.structure.MessageHeaders#SELECTION_FAILURE_REASON} will
 * be set with a value from {@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionFailureReason} as
 * follows:
 *
 * <ul>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionFailureReason#METADATA_ERROR}:
 *          An error occurred while trying to resolve the list of available versions.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionFailureReason#METADATA_MISSING}:
 *          Failed to find a list of available versions.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionFailureReason#OVERCONSTRAINED_VERSION}:
 *          The constraints placed on the version by the user's request or the relationship metadata excluded all
 *          available versions.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionFailureReason#UNDERCONSTRAINED_VERSION}:
 *          The constraints placed on the version by the user's request or the relationship metadata (if there were any)
 *          were insufficient to resolve ambiguity in the selection among the available versions.</li>
 * </ul>
 */
public interface NodeSelector
{
    @Handler
    void select( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
        throws Exception;

    CartoPackageInfo getPackageInfo();
}
