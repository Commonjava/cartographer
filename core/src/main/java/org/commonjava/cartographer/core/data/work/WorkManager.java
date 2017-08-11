package org.commonjava.cartographer.core.data.work;

import org.commonjava.cartgorapher.model.RequestId;

/**
 * Manager class responsible for storing / organizing state related to ongoing graph traversals.
 */
public interface WorkManager
{
    RequestWorkspace getWorkspace( RequestId requestId )
            throws Exception;

    WorkItem getWorkItem( WorkId workId )
            throws Exception;

    void addRequestWorkspace( RequestWorkspace ws );
}
