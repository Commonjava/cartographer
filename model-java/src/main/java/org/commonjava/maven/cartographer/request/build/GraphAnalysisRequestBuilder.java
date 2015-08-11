package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.request.AbstractGraphRequest;
import org.commonjava.maven.cartographer.request.GraphAnalysisRequest;
import org.commonjava.maven.cartographer.request.MultiGraphRequest;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GraphAnalysisRequestBuilder<T extends GraphAnalysisRequestBuilder<T>>
    implements GraphRequestOwner<T, MultiGraphRequest>
{

    private List<MultiGraphRequest> graphRequests = new ArrayList<>();

    public MultiGraphRequestBuilder newGraphRequestBuilder()
    {
        return new MultiGraphRequestBuilder( this );
    }

    @Override
    public T withGraphRequest( MultiGraphRequest request )
    {
        graphRequests.add( request );
        return (T) this;
    }

    public GraphAnalysisRequest build()
    {
        return new GraphAnalysisRequest(graphRequests);
    }

}
