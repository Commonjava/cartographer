package org.commonjava.cartographer.core.proc;

import org.apache.camel.Body;
import org.apache.camel.OutHeaders;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartographer.core.data.db.WorkDB;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkItem;
import org.commonjava.cartographer.core.structure.MessageHeaders;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

/**
 * Register the fact that we're about to resolve a {@link PkgVersion}, or if it's
 * already being resolved, avoid the resolve step in the pipeline.
 */
@ApplicationScoped
public class NodePreSelector
{
    @Inject
    private WorkDB workDB;

    public void handlePreSelect( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
            throws Exception
    {
        WorkItem item = workDB.getWorkItem( workId );
        PkgVersion selection = workDB.getVersionSelection( workId.getRequestId(), item.getTarget() );

        if ( selection != null )
        {
            outHeaders.put( MessageHeaders.SELECT_STATUS, MessageHeaders.SelectStatus.DONE );
        }
        else if ( workDB.isPendingSelection( workId.getRequestId(), item.getTarget() ) )
        {
            outHeaders.put( MessageHeaders.SELECT_STATUS, MessageHeaders.SelectStatus.SELECTING );
        }
        else
        {
            outHeaders.put( MessageHeaders.SELECT_STATUS, MessageHeaders.SelectStatus.UNKNOWN );
        }
    }
}
