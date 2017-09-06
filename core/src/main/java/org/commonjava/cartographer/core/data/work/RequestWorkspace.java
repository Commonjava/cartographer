package org.commonjava.cartographer.core.data.work;

import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartgorapher.model.RequestId;
import org.commonjava.cartgorapher.model.user.TraverseScope;
import org.commonjava.cartgorapher.model.user.UserRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This workspace contains state (accumulated and initial, from the user request) specific to this request.
 * <br/>
 * <b>NOTE:</b> This class may not need to exist explicitly. We will provide access to all of this information
 * via {@link org.commonjava.cartographer.core.data.db.WorkDB} to keep options open.
 */
public class RequestWorkspace
{
    private final RequestId requestId;

    private final UserRequest request;

    private final TraverseScope traversalScope;

    private final Set<PkgId> exclusions;

    private int currentDepth;

    private final int maxDepth;

    /**
     * Current batch of packages being resolved in the current BSP phase.
     */
    private Set<WorkId> pending = new HashSet<>();

    /**
     * All packages that have been processed, INCLUDING both traversed and discarded.
     * This is used to avoid re-traversing things.
     */
    private Set<PkgVersion> done = new HashSet<>();

    private final Map<PkgId, PkgVersion> selectedVersions;

    /**
     * Set of {@link PkgId} waiting for node selection to happen. Once selected, the pkgId gets removed from
     * here and mapped to a {@link PkgVersion} in {@link #selectedVersions}.
     */
    private final Set<PkgId> pendingSelection = new HashSet<>();

    private Set<WorkId> errors = new HashSet<>();

    public RequestWorkspace( final UserRequest request )
    {
        this.request = request;
        requestId = new RequestId();

        Map<PkgId, PkgVersion> reqVersions = request.getSelectedVersions();
        if ( reqVersions == null )
        {
            reqVersions = Collections.emptyMap();
        }

        this.exclusions = request.getExclusions();
        this.traversalScope = request.getTraverseScope();
        this.maxDepth = request.getMaxDepth();
        this.selectedVersions = new ConcurrentHashMap<>( reqVersions );
    }

    public RequestId getRequestId()
    {
        return requestId;
    }

    public PkgVersion getVersionSelection( PkgId id )
    {
        return selectedVersions.get( id );
    }

    public PkgVersion addVersionSelection( PkgId id, PkgVersion version )
    {
        return selectedVersions.computeIfAbsent( id, pkgId -> version );
    }

    public void addWorkItems( final List<WorkItem> nextItems )
    {
        nextItems.forEach( item->pending.add(item.getWorkId()) );
    }

    public void markDone( WorkItem workItem )
    {
        pending.remove( workItem.getWorkId() );
        if ( workItem.getError() != null )
        {
            errors.add( workItem.getWorkId() );
        }

        if ( workItem.getSelected() != null )
        {
            done.add( workItem.getSelected() );
        }
    }

    public boolean isPendingSelection( PkgId pkgId )
    {
        return pendingSelection.contains( pkgId );
    }
}
