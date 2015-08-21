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
package org.commonjava.cartographer.graph.fn;

import java.util.Set;
import java.util.function.Supplier;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MultiGraphAllInput
{

    private final Supplier<Set<ProjectVersionRef>> allRefs;

    private final Supplier<Set<ProjectRelationship<?>>> allRels;

    private final Supplier<Set<ProjectVersionRef>> roots;

    public MultiGraphAllInput( final Supplier<Set<ProjectVersionRef>> allRefs,
                               final Supplier<Set<ProjectRelationship<?>>> allRels,
                               final Supplier<Set<ProjectVersionRef>> roots )
    {
        this.allRefs = allRefs;
        this.allRels = allRels;
        this.roots = roots;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return allRefs.get();
    }

    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return allRels.get();
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots.get();
    }

}
