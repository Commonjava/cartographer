package org.commonjava.cartographer.core.structure;

/**
 * Created by jdcasey on 7/17/17.
 */
public final class MessageHeaders
{
    public static final String BSP_PHASE_CONTROL = "bsp-phase-control";
    public static final String BSP_DONE_REASON = "bsp-done-reason";

    public static final String SELECTION_RESULT = "selection-result";
    public static final String SELECTION_FAILURE_REASON = "selection-failure-reason";

    public static final String RESOLUTION_RESULT = "resolution-result";

    public static final String TRAVERSAL_RESULT = "traversal-result";

    public enum TraversalResult
    {
        TRAVERSAL_DONE;
    }

    /**
     * Possible result states for the {@link org.commonjava.cartographer.spi.service.NodeResolver} processor.
     */
    public enum ResolutionResult
    {
        /**
         * Resolution failed because metadata couldn't be parsed, or repo server gave non-recoverable error response.
         */
        RESOLUTION_ERROR,
        /**
         * Resolution failed to find the metadata on the server (or locally), or repo server gave recoverable error
         * response.
         */
        RESOLUTION_FAILED,
        /**
         * Resolution result was already available in the
         * {@link org.commonjava.cartographer.core.data.db.GraphDB}
         */
        RESOLUTION_AVOIDED,
        /**
         * Resolution succeeded normally, and results are now available in the
         * {@link org.commonjava.cartographer.core.data.db.GraphDB}
         */
        RESOLUTION_DONE;
    }

    /**
     * Possible reasons for failure of the {@link org.commonjava.cartographer.spi.service.NodeSelector} processor.
     */
    public enum SelectionFailureReason
    {
        /**
         * Cannot parse / read metadata in order to extract a version list for selection.
         */
        METADATA_ERROR,
        /**
         * Cannot find metadata in order to generate a version list for selection.
         */
        METADATA_MISSING,
        /**
         * Constraints in version advice have excluded all available versions.
         */
        OVERCONSTRAINED_VERSION,
        /**
         * Constraints (or lack thereof) in version advice have failed to resolve ambiguity among versions available
         * for selection.
         */
        UNDERCONSTRAINED_VERSION;
    }

    /**
     * Possible result states for the {@link org.commonjava.cartographer.spi.service.NodeSelector} processor.
     */
    public enum SelectionResult
    {
        /**
         * Indicates the incoming {@link org.commonjava.cartographer.core.data.work.WorkItem} already contains a selected
         * {@link org.commonjava.cartgorapher.model.graph.PkgVersion}, as will be the case in the first BSP phase,
         * when the user provides a concrete root node for the traverse.
         */
        SELECTION_AVOIDED,
        /**
         * Selection succeeded normally.
         */
        SELECTION_DONE,
        /**
         * Failed to select a {@link org.commonjava.cartgorapher.model.graph.PkgVersion} given the
         * {@link org.commonjava.cartgorapher.model.graph.PkgId} and available version advice.
         */
        SELECTION_FAILED;
    }

    /**
     * If a BSP traverse is marked as complete by {@link org.commonjava.cartographer.spi.service.BSPBoundaryProcessor},
     * this is the reason processing has stopped.
     */
    public enum BspDoneReason
    {
        /**
         * Traversal has completed; there are no new nodes to process.
         */
        TRAVERSE_COMPLETE,
        /**
         * Traversal may not have reached a natural conclusion (i.e. there may still be nodes in scope left to
         * traverse). However, the user has specified a maximum traverse depth, and the current BSP process has reached
         * that depth.
         */
        DEPTH_LIMIT_REACHED;
    }

    /**
     * Control directives resulting from {@link org.commonjava.cartographer.spi.service.BSPBoundaryProcessor} execution.
     * These will determine whether to start a new BSP processing phase, let the current one continue, or stop
     * processing altogether.
     */
    public enum BspPhaseControl
    {
        /**
         * All current {@link org.commonjava.cartographer.core.data.work.WorkItem}s in the phase are complete, but there
         * are more items in the next phase ready to process. Start the next phase.
         */
        START_NEXT_PHASE,
        /**
         * There are {@link org.commonjava.cartographer.core.data.work.WorkItem}s remaining in the current BSP phase;
         * do nothing for now, and allow these to complete.
         */
        CURRENT_PHASE_IN_PROGRESS,
        /**
         * The current BSP phase is complete, and there are no new {@link org.commonjava.cartographer.core.data.work.WorkItem}s
         * awaiting processing in a next phase. BSP processing for this traverse is complete.
         */
        BSP_DONE;
    }

    private MessageHeaders(){}
}