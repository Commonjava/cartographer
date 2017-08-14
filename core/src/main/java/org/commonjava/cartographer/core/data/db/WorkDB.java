package org.commonjava.cartographer.core.data.db;

import org.commonjava.cartgorapher.model.RequestId;
import org.commonjava.cartographer.core.data.work.RequestWorkspace;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkItem;

/**
 * Manager class responsible for storing / organizing state related to ongoing graph traversals.
 */
public interface WorkDB
{
    RequestWorkspace getWorkspace( RequestId requestId )
            throws Exception;

    WorkItem getWorkItem( WorkId workId )
            throws Exception;

    void addRequestWorkspace( RequestWorkspace ws );
}
