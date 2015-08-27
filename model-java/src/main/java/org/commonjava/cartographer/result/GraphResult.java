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
