/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
