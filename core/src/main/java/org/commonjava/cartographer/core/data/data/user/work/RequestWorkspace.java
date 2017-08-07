package org.commonjava.cartographer.core.data.data.user.work;

import org.commonjava.cartographer.core.data.data.model.PkgId;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.user.TraverseScope;
import org.commonjava.cartographer.core.data.data.user.UserRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This workspace contains state (accumulated and initial, from the user request) specific to this request.
 */
public class RequestWorkspace
{
    private final RequestId requestId;

    private final UserRequest request;

    private final TraverseScope traversalScope;

    /**
     * Next batch of packages to resolve in BSP phase
     */
    private List<PkgId> discovered;

    /**
     * Current batch of packages being resolved.
     */
    private Set<PkgVersion> selected;

    /**
     * All packages that have been processed, INCLUDING both traversed and discarded.
     * This is used to avoid re-traversing things.
     */
    private Set<PkgVersion> done;

    private final Map<PkgId, PkgVersion> selectedVersions;

    public RequestWorkspace( final UserRequest request )
    {
        this.request = request;
        requestId = new RequestId();

        Map<PkgId, PkgVersion> reqVersions = request.getSelectedVersions();
        if ( reqVersions == null )
        {
            reqVersions = Collections.emptyMap();
        }

        this.traversalScope = request.getTraverseScope();
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
}
