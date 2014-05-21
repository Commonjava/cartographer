/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
