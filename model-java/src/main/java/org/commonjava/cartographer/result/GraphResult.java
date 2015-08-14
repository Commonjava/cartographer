package org.commonjava.cartographer.result;

import org.commonjava.cartographer.request.GraphDescription;

public class GraphResult<M>
{
    private GraphDescription originalGraph;

    private GraphDescription resolvedGraph;

    private M result;

    public GraphResult()
    {
    }

    public GraphResult( final GraphDescription original, final GraphDescription resolved, final M result )
    {
        this.originalGraph = original;
        this.resolvedGraph = resolved;
        this.result = result;
    }

    public GraphDescription getOriginalGraph()
    {
        return originalGraph;
    }

    public void setOriginalGraph( final GraphDescription originalGraph )
    {
        this.originalGraph = originalGraph;
    }

    public GraphDescription getResolvedGraph()
    {
        return resolvedGraph;
    }

    public void setResolvedGraph( final GraphDescription resolvedGraph )
    {
        this.resolvedGraph = resolvedGraph;
    }

    public M getResult()
    {
        return result;
    }

    public void setResult( final M result )
    {
        this.result = result;
    }

}
