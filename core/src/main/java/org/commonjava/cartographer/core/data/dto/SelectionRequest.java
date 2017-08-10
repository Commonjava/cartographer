package org.commonjava.cartographer.core.data.dto;

import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartgorapher.model.RequestId;

/**
 * Created by jdcasey on 7/31/17.
 */
public class SelectionRequest
{
    private final RequestId requestId;

    private final PkgVersion fromNode;

    private final PkgId toPackage;

    private final String originalVersionAdvice;

    public SelectionRequest( final RequestId requestId, final PkgVersion fromNode, final PkgId toPackage,
                             final String originalVersionAdvice )
    {
        this.requestId = requestId;
        this.fromNode = fromNode;
        this.toPackage = toPackage;
        this.originalVersionAdvice = originalVersionAdvice;
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public PkgVersion getFromNode()
    {
        return fromNode;
    }

    public PkgId getToPackage()
    {
        return toPackage;
    }

    public String getOriginalVersionAdvice()
    {
        return originalVersionAdvice;
    }
}
