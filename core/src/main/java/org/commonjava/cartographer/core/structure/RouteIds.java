package org.commonjava.cartographer.core.structure;

/**
 * Routes involved in a BSP processing step.
 */
public enum RouteIds
{
    /**
     * Select which {@link org.commonjava.cartgorapher.model.graph.PkgVersion} to use, based on provided version advice
     * and available versions.
     */
    SELECT,
    /**
     * Resolve relationships from repository metadata for a given, selected
     * {@link org.commonjava.cartgorapher.model.graph.PkgVersion}
     */
    RESOLVE,
    /**
     * Select which relationships to include in a graph traversal, then provide them for the next BSP processing phase.
     */
    TRAVERSE,
    /**
     * Check whether the current BSP phase is done, and whether a new phase needs to be started.
     */
    BSP_BOUNDARY
    ;
}
