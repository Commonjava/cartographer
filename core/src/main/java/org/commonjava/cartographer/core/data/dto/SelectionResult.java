package org.commonjava.cartographer.core.data.dto;

import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartgorapher.model.RequestId;

/**
 * Created by jdcasey on 8/8/17.
 */
public class SelectionResult
{
    private final RequestId requestId;

    private final PkgVersion fromNode;

    private final PkgVersion toNode;

    public SelectionResult( SelectionRequest request, PkgVersion toNode )
    {
        this.requestId = request.getRequestId();
        this.fromNode = request.getFromNode();
        this.toNode = toNode;
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public PkgVersion getFromNode()
    {
        return fromNode;
    }

    public PkgVersion getToNode()
    {
        return toNode;
    }
}
