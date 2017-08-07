package org.commonjava.cartographer.core.data.data.global.dto;

import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.user.work.RequestId;

/**
 * Represents a resolved package version that needs to be traversed.
 */
public class TraversalRequest
{
    private final RequestId requestId;

    private final PkgVersion packageVersion;

    public TraversalRequest( final RequestId requestId, final PkgVersion packageVersion )
    {
        this.requestId = requestId;
        this.packageVersion = packageVersion;
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public PkgVersion getPackageVersion()
    {
        return packageVersion;
    }
}
