package org.commonjava.cartographer.data.global.dto;

import org.commonjava.cartographer.data.model.PkgVersion;
import org.commonjava.cartographer.data.user.work.RequestId;

/**
 * Created by jdcasey on 7/31/17.
 */
public class ResolutionRequest
{
    private final RequestId requestId;

    private final PkgVersion packageVersion;

    public ResolutionRequest( final RequestId requestId, final PkgVersion packageVersion )
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
