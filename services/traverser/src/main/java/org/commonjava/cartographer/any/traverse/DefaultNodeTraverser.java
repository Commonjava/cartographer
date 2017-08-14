package org.commonjava.cartographer.any.traverse;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.Relationship;
import org.commonjava.cartgorapher.model.user.TraverseScope;
import org.commonjava.cartographer.core.data.db.GraphDB;
import org.commonjava.cartographer.core.data.work.RequestWorkspace;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkItem;
import org.commonjava.cartographer.core.data.db.WorkDB;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.spi.service.NodeTraverser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @see org.commonjava.cartographer.spi.service.NodeTraverser
 */
@ApplicationScoped
public class DefaultNodeTraverser
        implements NodeTraverser
{
    @Inject
    private GraphDB graphDB;

    @Inject
    private WorkDB workManager;

    @Handler
    @Override
    public void traverseNode( final @Body WorkId workId, final @OutHeaders Map<String, Object> outHeaders )
            throws Exception
    {
        RequestWorkspace workspace = workManager.getWorkspace( workId.getRequestId() );
        WorkItem workItem = workManager.getWorkItem( workId );

        AtomicInteger count = new AtomicInteger( 0 );

        List<WorkItem> nextItems = new ArrayList<>();

        graphDB.getRelationshipsFromVersion( workItem.getSelected() )
               .forEach( rel -> processRelationship( rel, workId, count, workItem, nextItems ) );

        workspace.markDone( workItem );
        workspace.addNextItems( nextItems );

        outHeaders.put( MessageHeaders.TRAVERSAL_RESULT, MessageHeaders.TraversalResult.TRAVERSAL_DONE );
    }

    private void processRelationship( final Relationship rel, final WorkId workId, final AtomicInteger count,
                                      final WorkItem workItem, final List<WorkItem> nextItems )
    {
        if ( !rel.getSupportedTraverseScopes().contains( workItem.getScope() ) )
        {
            // skip this relationship....wrong scope.
            return;
        }

        if ( workItem.getExclusions().contains( rel.getTarget() ) )
        {
            // skip this relationship...excluded.
            return;
        }

        String nextInt = Integer.toString( count.getAndIncrement() );
        WorkId nextId = new WorkId( workId.getRequestId(), workId.getItemId() + "." + nextInt );

        TraverseScope nextScope = TraverseScope.runtime;

        Set<PkgId> nextExclusions = new HashSet<>();
        nextExclusions.addAll( workItem.getExclusions() );
        nextExclusions.addAll( rel.getExclusions() );

        nextItems.add( new WorkItem( nextId, rel.getTarget(), rel.getTargetVersionAdvice(), nextScope, nextExclusions,
                                     workId ) );
    }
}
