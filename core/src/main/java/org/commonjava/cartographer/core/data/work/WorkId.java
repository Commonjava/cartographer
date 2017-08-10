package org.commonjava.cartographer.core.data.work;

import org.commonjava.cartgorapher.model.RequestId;

/**
 * Created by jdcasey on 8/9/17.
 */
public class WorkId
{
    private final RequestId requestId;

    private final String itemId;

    public WorkId( RequestId requestId, String itemId )
    {
        this.requestId = requestId;
        this.itemId = itemId;
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public String getItemId()
    {
        return itemId;
    }

    @Override
    public String toString()
    {
        return "WorkId{" + "requestId=" + requestId + ", itemId='" + itemId + '\'' + '}';
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof WorkId ) )
        {
            return false;
        }

        final WorkId workId = (WorkId) o;

        if ( !requestId.equals( workId.requestId ) )
        {
            return false;
        }
        return itemId.equals( workId.itemId );
    }

    @Override
    public int hashCode()
    {
        int result = requestId.hashCode();
        result = 31 * result + itemId.hashCode();
        return result;
    }
}
