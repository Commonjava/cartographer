package org.commonjava.cartgorapher.model;

import java.util.UUID;

/**
 * Created by jdcasey on 7/31/17.
 */
public class RequestId
{
    private String id;

    public RequestId()
    {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String toString()
    {
        return "RequestId[" + id + ']';
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof RequestId ) )
        {
            return false;
        }

        final RequestId requestId = (RequestId) o;

        return id != null ? id.equals( requestId.id ) : requestId.id == null;
    }

    @Override
    public int hashCode()
    {
        return id != null ? id.hashCode() : 0;
    }
}
