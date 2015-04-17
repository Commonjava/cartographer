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
package org.commonjava.maven.cartographer.dto;

import java.util.Set;

public class GraphDifference<T>
{

    private Set<T> added;

    private Set<T> removed;

    private GraphDescription toGraph;

    private GraphDescription fromGraph;

    protected GraphDifference()
    {
    }

    public GraphDifference( final GraphDescription fromGraph, final GraphDescription toGraph, final Set<T> added,
                            final Set<T> removed )
    {
        this.fromGraph = fromGraph;
        this.toGraph = toGraph;
        this.added = added;
        this.removed = removed;
    }

    public Set<T> getAdded()
    {
        return added;
    }

    public Set<T> getRemoved()
    {
        return removed;
    }

    public GraphDescription getToGraph()
    {
        return toGraph;
    }

    public GraphDescription getFromGraph()
    {
        return fromGraph;
    }

    protected void setAdded( final Set<T> added )
    {
        this.added = added;
    }

    protected void setRemoved( final Set<T> removed )
    {
        this.removed = removed;
    }

    protected void setToGraph( final GraphDescription toGraph )
    {
        this.toGraph = toGraph;
    }

    protected void setFromGraph( final GraphDescription fromGraph )
    {
        this.fromGraph = fromGraph;
    }

}
