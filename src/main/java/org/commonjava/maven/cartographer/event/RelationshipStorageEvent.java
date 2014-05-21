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

import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class RelationshipStorageEvent
{

    private final Set<ProjectRelationship<?>> stored;

    private final Set<ProjectRelationship<?>> rejected;

    public RelationshipStorageEvent( final Set<ProjectRelationship<?>> stored,
                                     final Set<ProjectRelationship<?>> rejected )
    {
        this.stored = stored;
        this.rejected = rejected;
    }

    public final Set<ProjectRelationship<?>> getStored()
    {
        return stored;
    }

    public final Set<ProjectRelationship<?>> getRejected()
    {
        return rejected;
    }

}
