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
package org.commonjava.maven.cartographer.result;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.Set;

public class GraphExport
{

    private Set<ProjectRelationship<?>> relationships;

    private Set<ProjectVersionRef> missing;

    private Set<ProjectVersionRef> variable;

    private ProjectErrors errors;

    private final Set<EProjectCycle> cycles;

    public GraphExport( final Set<ProjectRelationship<?>> relationships,
                        final Set<ProjectVersionRef> missingProjects,
                        final Set<ProjectVersionRef> variableProjects,
                        final ProjectErrors projectErrors,
                        final Set<EProjectCycle> cycles )
    {
        this.relationships = relationships;
        this.missing = missingProjects;
        this.variable = variableProjects;
        this.errors = projectErrors;
        this.cycles = cycles;
    }

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( final Set<ProjectRelationship<?>> relationships )
    {
        this.relationships = relationships;
    }

    public Set<ProjectVersionRef> getMissing()
    {
        return missing;
    }

    public void setMissing( final Set<ProjectVersionRef> missing )
    {
        this.missing = missing;
    }

    public Set<ProjectVersionRef> getVariable()
    {
        return variable;
    }

    public void setVariable( final Set<ProjectVersionRef> variable )
    {
        this.variable = variable;
    }

    public ProjectErrors getErrors()
    {
        return errors;
    }

    public void setErrors( final ProjectErrors errors )
    {
        this.errors = errors;
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

}
