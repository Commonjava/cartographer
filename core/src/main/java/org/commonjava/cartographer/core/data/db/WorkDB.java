package org.commonjava.cartographer.core.data.db;

import org.commonjava.cartgorapher.model.RequestId;
import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartographer.core.data.work.RequestWorkspace;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkItem;
import org.commonjava.cartographer.core.structure.MessageHeaders;

import java.util.List;

/**
 * Manager class responsible for storing / organizing state related to ongoing graph traversals.
 */
public interface WorkDB
{
    // FIXME: Remove this?
    RequestWorkspace getWorkspace( RequestId requestId )
            throws Exception;

    // FIXME: Remove this? Or will we just take it apart and store the parts for our own methods to access?
    void addRequestWorkspace( RequestWorkspace ws );

    WorkItem getWorkItem( WorkId workId )
            throws Exception;

    boolean isPendingSelection( RequestId requestId, PkgId pkgId )
            throws Exception;

    PkgVersion getVersionSelection( RequestId requestId, PkgId id );

    PkgVersion addVersionSelection( RequestId requestId, PkgId id, PkgVersion version );

    void addWorkItems( RequestId requestId, List<WorkItem> nextItems );

    void markDone( RequestId requestId, WorkItem workItem );

    boolean isResolutionPending( PkgVersion version );

    void addPendingResolution( PkgVersion version );
}
