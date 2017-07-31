package org.commonjava.cartographer.data.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.commonjava.cartographer.data.model.PkgId;
import org.commonjava.cartographer.data.model.PkgVersion;

import java.util.List;
import java.util.Map;

/**
 * This is the DTO sent by the user to configure the graph traverse.
 */
public class UserRequest
{
    @JsonProperty( "roots" )
    private final List<PkgVersion> traversalRootNodes;

    @JsonProperty( "versions" )
    private Map<PkgId, PkgVersion> selectedVersions;

    @JsonProperty( "result" )
    private ResultType resultType;

    @JsonProperty( "scope" )
    private TraverseScope traverseScope;

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
}
