package org.commonjava.cartographer.spi;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.user.work.RequestId;
import org.commonjava.cartographer.core.structure.MessageHeaders;

/**
 * This is responsible for adding the given node to the results of any user requests that are interested in it.
 */
public class NodeResultAggregator
{
    @Handler
    public void registerResult( final @Header( MessageHeaders.REQUEST_ID ) RequestId requestId, final @Body
            PkgVersion packageVersion )
            throws Exception
    {
    }
}
