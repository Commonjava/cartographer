package org.commonjava.cartographer.core.structure;

/**
 * Created by jdcasey on 7/10/17.
 */
public final class Routes
{
    public static final String ROUTE_RESOLVE_NODE_LOOKUP = "resolve-node";

    public static final String ROUTE_SELECT_NODE_LOOKUP = "select-node";

    public static final String ROUTE_TRAVERSE_NODE = "traverse-node";

    public static final String ROUTE_RECORD_NODE_RESULTS = "record-node-results";

    public static final String ROUTE_PHASE_CHECK = "phase-check";

    public static final String ROUTE_END_REQUEST = "end-request";



    public static final String ROUTE_RESOLVER_NOT_FOUND = "resolver-not-found";

    public static final String ROUTE_SELECTOR_NOT_FOUND = "selector-not-found";

    public static final String ROUTE_RESOLVER_ERROR = "resolve-error";

    public static final String ROUTE_SELECTOR_ERROR = "select-error";

    public static final String ROUTE_NODE_CYCLE_ERROR = "node-cycle-error";

    public static final String ROUTE_PROGRESS_NODE_RESOLVED = "progress-node-resolved";

    public static final String ROUTE_PROGRESS_NODE_TRAVERSED = "progress-node-analyzed";

    public static final String ROUTE_PROGRESS_NODE_ADDED = "progress-node-added";

    public static final String ROUTE_ADD_NODE_PROGRESS = "add-node-progress";

    private Routes(){}
}
