package org.commonjava.cartographer.data.global.dto;

import org.commonjava.cartographer.data.model.PkgId;
import org.commonjava.cartographer.data.model.PkgVersion;
import org.commonjava.cartographer.data.user.work.RequestId;

import java.util.Map;

/**
 * Created by jdcasey on 7/31/17.
 */
public class SelectionRequest
{
    private final RequestId requestId;

    private final PkgVersion source;

    private final PkgId target;

    private final String nativeVersionAdvice;

    public SelectionRequest( final RequestId requestId, final PkgVersion source, final PkgId target,
                             final String nativeVersionAdvice )
    {
        this.requestId = requestId;
        this.source = source;
        this.target = target;
        this.nativeVersionAdvice = nativeVersionAdvice;
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public PkgVersion getSource()
    {
        return source;
    }

    public PkgId getTarget()
    {
        return target;
    }

    public String getNativeVersionAdvice()
    {
        return nativeVersionAdvice;
    }
}
