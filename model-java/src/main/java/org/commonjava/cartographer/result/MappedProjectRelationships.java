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

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class MappedProjectRelationships
{

    private ProjectVersionRef project;

    private Set<ProjectRelationship<?>> relationships;

    public MappedProjectRelationships()
    {
    }

    public MappedProjectRelationships( ProjectVersionRef project, Set<ProjectRelationship<?>> relationships )
    {
        this.project = project;
        this.relationships = relationships;

    }

    public ProjectVersionRef getProject()
    {
        return project;
    }

    public void setProject( ProjectVersionRef project )
    {
        this.project = project;
    }

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( Set<ProjectRelationship<?>> relationships )
    {
        this.relationships = relationships;
    }
}
