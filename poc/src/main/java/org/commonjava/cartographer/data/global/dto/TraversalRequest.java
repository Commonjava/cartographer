package org.commonjava.cartographer.data.global.dto;

import org.commonjava.cartographer.data.model.PkgId;
import org.commonjava.cartographer.data.model.PkgVersion;
import org.commonjava.cartographer.data.model.RelationshipId;
import org.commonjava.cartographer.data.user.work.RequestId;

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
