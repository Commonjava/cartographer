package org.commonjava.cartgorapher.model.user;

/**
 * This captures the different scopes of relationships we should include in results (and in traversals of the graph).
 */
public enum TraverseScope
{
    runtime,
    build_time;
}
