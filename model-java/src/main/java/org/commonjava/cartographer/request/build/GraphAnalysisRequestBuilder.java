/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
