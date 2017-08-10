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

    public enum ResolutionResult
    {
        RESOLUTION_ERROR,
        RESOLUTION_FAILED,
        RESOLUTION_DONE;
    }

    public enum SelectionFailureReason
    {
        METADATA_ERROR,
        METADATA_MISSING,
        OVERCONSTRAINED_VERSION,
        UNDERCONSTRAINED_VERSION;
    }

    public enum SelectionResult
    {
        DONE,
        SELECTION_FAILED;
    }

    public enum BspDoneReason
    {
        TRAVERSE_COMPLETE,
        DEPTH_LIMIT_REACHED;
    }

    public enum BspPhaseControl
    {
        START_NEXT_PHASE,
        CURRENT_PHASE_IN_PROGRESS,
        BSP_DONE;
    }

    private MessageHeaders(){}
}
