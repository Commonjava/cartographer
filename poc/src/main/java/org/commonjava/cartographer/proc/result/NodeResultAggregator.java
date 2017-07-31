package org.commonjava.cartographer.proc.result;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.commonjava.cartographer.data.model.PkgId;
import org.commonjava.cartographer.data.model.PkgVersion;
import org.commonjava.cartographer.data.user.work.RequestId;
import org.commonjava.cartographer.structure.MessageHeaders;

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
