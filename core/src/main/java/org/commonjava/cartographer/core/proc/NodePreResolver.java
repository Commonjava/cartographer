package org.commonjava.cartographer.core.proc;

import org.apache.camel.Body;
import org.apache.camel.OutHeaders;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartographer.core.data.db.GraphDB;
import org.commonjava.cartographer.core.data.db.WorkDB;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkItem;
import org.commonjava.cartographer.core.structure.MessageHeaders;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

/**
 * Register the fact that we're about to resolve a {@link org.commonjava.cartgorapher.model.graph.PkgVersion}, or if it's
 * already being resolved, avoid the resolve step in the pipeline.
 */
@ApplicationScoped
public class NodePreResolver
{
    @Inject
    private WorkDB workDB;

    @Inject
    private GraphDB graphDB;

    public void handlePreResolve( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
            throws Exception
    {
        WorkItem item = workDB.getWorkItem( workId );
        PkgVersion selected = item.getSelected();

        if ( graphDB.isResolved( selected ) )
        {
            outHeaders.put( MessageHeaders.RESOLVE_STATUS, MessageHeaders.ResolutionState.DONE );
        }
        else if ( workDB.isResolutionPending( selected ) )
        {
            outHeaders.put( MessageHeaders.RESOLVE_STATUS, MessageHeaders.ResolutionState.RESOLVING );
        }

        outHeaders.put( MessageHeaders.RESOLVE_STATUS, MessageHeaders.ResolutionState.UNRESOLVED );
    }
}
