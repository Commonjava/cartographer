package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.MultiGraphRequest;

import java.util.*;

public class GraphAnalysisRequestBuilder<T extends GraphAnalysisRequestBuilder<T>>
{

    private List<MultiGraphRequest> graphRequests = new ArrayList<>();

    public static final class StandaloneAnalysisBuilder
                    extends GraphAnalysisRequestBuilder<StandaloneAnalysisBuilder>
    {
    }

    public static StandaloneAnalysisBuilder newAnalysisRequestBuilder()
    {
        return new StandaloneAnalysisBuilder();
    }

    public T withGraphRequest( MultiGraphRequest request )
    {
        graphRequests.add( request );
        return (T) this;
    }

    public GraphAnalysisRequest build()
    {
        return new GraphAnalysisRequest( graphRequests );
    }

}
