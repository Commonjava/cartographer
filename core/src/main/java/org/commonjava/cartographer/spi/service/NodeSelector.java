package org.commonjava.cartographer.spi.service;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;

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
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#AVOIDED}:
 *          The incoming {@link org.commonjava.cartographer.core.data.work.WorkItem} already contains a fully
 *          selected {@link org.commonjava.cartgorapher.model.graph.PkgVersion}. This will happen with the initial
 *          BSP phase from a user request, since the user normally will have specified the version for the root
 *          nodes of the traverse.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#DONE}: Resolved the
 *          {@link org.commonjava.cartgorapher.model.graph.PkgId} to a {@link org.commonjava.cartgorapher.model.graph.PkgVersion},
 *          which is now ready to resolve.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#FAILED}:
 *          Could not find a valid {@link org.commonjava.cartgorapher.model.graph.PkgVersion} selection for the given
 *          {@link org.commonjava.cartgorapher.model.graph.PkgId} that satisfied version constraints.</li>
 * </ul>
 *
 * If {@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#FAILED} is returned,
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
 *
 * <b>NOTE:</b> The startup of the BSP process will begin with a set of nodes that are fully selected (not just
 * {@link org.commonjava.cartgorapher.model.graph.PkgId} + version string, but a full
 * {@link org.commonjava.cartgorapher.model.graph.PkgVersion}), which come directly from the user request. When the
 * inbound WorkItem already contains a selected {@link org.commonjava.cartgorapher.model.graph.PkgVersion}, this
 * processor should pass through to the
 * {@link org.commonjava.cartographer.core.structure.MessageHeaders.SelectionResult#DONE} result.
 */
public interface NodeSelector
{
    @Handler
    void select( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
        throws Exception;

    CartoPackageInfo getPackageInfo();
}
