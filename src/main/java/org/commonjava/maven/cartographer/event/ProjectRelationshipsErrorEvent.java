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
package org.commonjava.maven.cartographer.event;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;


public class ProjectRelationshipsErrorEvent
{

    private final ProjectVersionRef ref;

    private final Throwable error;

    private final RelationshipGraph graph;

    public ProjectRelationshipsErrorEvent( final RelationshipGraph graph, final ProjectVersionRef ref,
                                           final Throwable error )
    {
        this.ref = ref;
        this.error = error;
        this.graph = graph;
    }

    public RelationshipGraph getGraph()
    {
        return graph;
    }

    public ProjectVersionRef getRef()
    {
        return ref;
    }

    public Throwable getError()
    {
        return error;
    }

}
