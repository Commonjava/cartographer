package org.commonjava.cartgorapher.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.PkgVersion;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the DTO sent by the user to configure the graph traverse.
 */
public class UserRequest
{
    @JsonProperty( "workspaceId" )
    private String requestId;

    @JsonProperty( "roots" )
    private final List<PkgVersion> traversalRootNodes;

    @JsonProperty( "versions" )
    private Map<PkgId, PkgVersion> selectedVersions;

    @JsonProperty( "result" )
    private ResultType resultType;

    @JsonProperty( "scope" )
    private TraverseScope traverseScope;

    @JsonProperty( "depth" )
    private int maxDepth;

    private Set<PkgId> exclusions;

    @JsonCreator
    public UserRequest( @JsonProperty( "roots" ) final List<PkgVersion> traversalRootNodes )
    {
        this.traversalRootNodes = traversalRootNodes;
    }

    public List<PkgVersion> getTraversalRootNodes()
    {
        return traversalRootNodes;
    }

    public Map<PkgId, PkgVersion> getSelectedVersions()
    {
        return selectedVersions;
    }

    public void setSelectedVersions( final Map<PkgId, PkgVersion> selectedVersions )
    {
        this.selectedVersions = selectedVersions;
    }

    public ResultType getResultType()
    {
        return resultType;
    }

    public void setResultType( final ResultType resultType )
    {
        this.resultType = resultType;
    }

    public TraverseScope getTraverseScope()
    {
        return traverseScope;
    }

    public void setTraverseScope( final TraverseScope traverseScope )
    {
        this.traverseScope = traverseScope;
    }

    public int getMaxDepth()
    {
        return maxDepth;
    }

    public void setMaxDepth( final int maxDepth )
    {
        this.maxDepth = maxDepth;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId( final String requestId )
    {
        this.requestId = requestId;
    }

    public Set<PkgId> getExclusions()
    {
        return exclusions == null ? Collections.emptySet() : exclusions;
    }

    public void setExclusions( final Set<PkgId> exclusions )
    {
        this.exclusions = exclusions;
    }
}
