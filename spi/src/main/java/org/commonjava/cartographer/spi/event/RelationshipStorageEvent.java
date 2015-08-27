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
package org.commonjava.cartographer.spi.event;

import java.util.Collection;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class RelationshipStorageEvent
{

    private final Collection<? extends ProjectRelationship<?, ?>> stored;

    private final Collection<ProjectRelationship<?, ?>> rejected;

    private final RelationshipGraph graph;

    public RelationshipStorageEvent( final Collection<? extends ProjectRelationship<?, ?>> relationships,
                                     final Collection<ProjectRelationship<?, ?>> rejected, final RelationshipGraph graph )
    {
        this.stored = relationships;
        this.rejected = rejected;
        this.graph = graph;
    }

    public final RelationshipGraph getGraph()
    {
        return graph;
    }

    public final Collection<? extends ProjectRelationship<?, ?>> getStored()
    {
        return stored;
    }

    public final Collection<ProjectRelationship<?, ?>> getRejected()
    {
        return rejected;
    }

}
