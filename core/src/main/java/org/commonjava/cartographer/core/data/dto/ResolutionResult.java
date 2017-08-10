package org.commonjava.cartographer.core.data.dto;

import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartgorapher.model.RequestId;

/**
 * Represents a resolved package version that needs to be traversed.
 */
public class ResolutionResult
{
    private final RequestId requestId;

    private final PkgVersion fromNode;

    private final String nodeSourceUri;

    private final PkgVersion toTraverse;

    public ResolutionResult( final RequestId requestId, final PkgVersion fromNode, final PkgVersion toTraverse, final String nodeSourceUri )
    {
        this.requestId = requestId;
        this.fromNode = fromNode;
        this.toTraverse = toTraverse;
        this.nodeSourceUri = nodeSourceUri;
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public PkgVersion getFromNode()
    {
        return fromNode;
    }

    public PkgVersion getToTraverse()
    {
        return toTraverse;
    }

    public String getNodeSourceUri()
    {
        return nodeSourceUri;
    }
}
