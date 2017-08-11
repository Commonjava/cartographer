package org.commonjava.cartographer.core.structure;

/**
 * These are keys used to construct routes used in Cartographer without referring to endpoints on specific protocols.
 * This should allow us to redeploy Cartographer in a different topology simply by changing the way these aliases
 * resolve to specific endpoint URIs.
 *
 * @see org.commonjava.propulsor.deploy.camel.route.EndpointAliasManager (in org.commonjava.propulsor:propulsor-camel)
 */
public final class EndpointKeys
{

    /**
     * Generic route for resolving a {@link org.commonjava.cartgorapher.model.graph.PkgVersion}, without specifying
     * which package-specific resolver should be used. This should map to a RecipientList EIP handler that will locate
     * the appropriate package {@link org.commonjava.cartographer.spi.service.NodeResolver} and forward the request to
     * the corresponding package-specific resolver route.
     */
    public static final String ROUTE_RESOLVE_NODE_LOOKUP = "resolve-node";

    /**
     * Generic route for selecting a {@link org.commonjava.cartgorapher.model.graph.PkgVersion}, without specifying
     * which package-specific selector should be used. This should map to a RecipientList EIP handler that will locate
     * the appropriate package {@link org.commonjava.cartographer.spi.service.NodeSelector} and forward the request to
     * the corresponding package-specific resolver route.
     */
    public static final String ROUTE_SELECT_NODE_LOOKUP = "select-node";

    /**
     * Route for traversing the resolved relationships defined by the given
     * {@link org.commonjava.cartgorapher.model.graph.PkgVersion}, and determining which of these relationships should
     * be traversed next.
     */
    public static final String ROUTE_TRAVERSE_NODE = "traverse-node";

    /**
     * Route for capturing the results of selecting, resolving, and traversing a single
     * {@link org.commonjava.cartgorapher.model.graph.PkgVersion}. The processor on this route is responsible for
     * checking whether the current {@link org.commonjava.cartographer.core.data.work.WorkItem} represents the end of
     * processing for the current BSP phase, and whether to kick off a new BSP phase.
     */
    public static final String ROUTE_PHASE_BOUNDARY = "phase-boundary";

    /**
     * Route for signalling that a graph traverse is complete.
     */
    public static final String ROUTE_END_REQUEST = "end-request";

    /**
     * Route for signalling that no package-specific {@link org.commonjava.cartographer.spi.service.NodeResolver}
     * could be located to handle the given {@link org.commonjava.cartgorapher.model.graph.PkgVersion}.
     */
    public static final String ROUTE_RESOLVER_NOT_FOUND = "resolver-not-found";

    /**
     * Route for signalling that no package-specific {@link org.commonjava.cartographer.spi.service.NodeSelector}
     * could be located to handle the given {@link org.commonjava.cartgorapher.model.graph.PkgVersion}.
     */
    public static final String ROUTE_SELECTOR_NOT_FOUND = "selector-not-found";

    /**
     * Route for capturing errors or failures that occur while resolving a
     * {@link org.commonjava.cartgorapher.model.graph.PkgVersion}.
     */
    public static final String ROUTE_RESOLVER_ERROR = "resolve-error";

    /**
     * Route for capturing errors or failures that occur while selecting a specific
     * {@link org.commonjava.cartgorapher.model.graph.PkgVersion} from a
     * {@link org.commonjava.cartgorapher.model.graph.PkgId} and the given version advice.
     */
    public static final String ROUTE_SELECTOR_ERROR = "select-error";

    private EndpointKeys(){}
}
