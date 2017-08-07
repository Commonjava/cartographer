package org.commonjava.cartographer.core.data.data.global.dto;

import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.model.RelationshipId;
import org.commonjava.cartographer.core.data.data.user.work.RequestId;

import java.util.List;

/**
 * Describes the output from a traversal execution. This includes the request for which the traversal was executed, the
 * source PkgVersion that was traversed, whether the traversal occurred (if the node should be included in the result),
 * and the list of next-hop relationships to process.
 *
 * NOTE: This processor can perform filtering on the current node (includeNode) OR on the child nodes related to it
 * (nextRelationships).
 *
 * When a set of TraversalRequests result in TraversalResults that contain zero nextRelationships (in total), the overall
 * BSP traverse process is complete. This implies the use of a EIP Aggregator to coordinate BSP steps.
 */
public class TraversalResult
{
    private final RequestId requestId;

    private final PkgVersion source;

    private final boolean includeNode;

    private final List<RelationshipId> nextRelationships;

    public TraversalResult( final TraversalRequest request, final boolean includeNode, final List<RelationshipId> nextRelationships )
    {
        this.requestId = request.getRequestId();
        this.source = request.getPackageVersion();
        this.includeNode = includeNode;
        this.nextRelationships = nextRelationships;
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public PkgVersion getSource()
    {
        return source;
    }

    public boolean isIncludeNode()
    {
        return includeNode;
    }

    public List<RelationshipId> getNextRelationships()
    {
        return nextRelationships;
    }
}
