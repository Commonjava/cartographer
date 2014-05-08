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

import java.util.Collection;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class RelationshipStorageEvent
{

    private final Collection<? extends ProjectRelationship<?>> stored;

    private final Collection<ProjectRelationship<?>> rejected;

    private final RelationshipGraph graph;

    public RelationshipStorageEvent( final Collection<? extends ProjectRelationship<?>> relationships,
                                     final Collection<ProjectRelationship<?>> rejected, final RelationshipGraph graph )
    {
        this.stored = relationships;
        this.rejected = rejected;
        this.graph = graph;
    }

    public final RelationshipGraph getGraph()
    {
        return graph;
    }

    public final Collection<? extends ProjectRelationship<?>> getStored()
    {
        return stored;
    }

    public final Collection<ProjectRelationship<?>> getRejected()
    {
        return rejected;
    }

}
