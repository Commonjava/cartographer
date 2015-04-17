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

import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphExport
{

    private Set<ProjectRelationship<?>> relationships;

    private Set<ProjectVersionRef> missingProjects;

    private Set<ProjectVersionRef> variableProjects;

    private Map<ProjectVersionRef, String> projectErrors;

    public GraphExport( final Set<ProjectRelationship<?>> relationships, final Set<ProjectVersionRef> missingProjects,
                        final Set<ProjectVersionRef> variableProjects,
                        final Map<ProjectVersionRef, String> projectErrors )
    {
        this.relationships = relationships;
        this.missingProjects = missingProjects;
        this.variableProjects = variableProjects;
        this.projectErrors = projectErrors;
    }

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( final Set<ProjectRelationship<?>> relationships )
    {
        this.relationships = relationships;
    }

    public Set<ProjectVersionRef> getMissingProjects()
    {
        return missingProjects;
    }

    public void setMissingProjects( final Set<ProjectVersionRef> missingProjects )
    {
        this.missingProjects = missingProjects;
    }

    public Set<ProjectVersionRef> getVariableProjects()
    {
        return variableProjects;
    }

    public void setVariableProjects( final Set<ProjectVersionRef> variableProjects )
    {
        this.variableProjects = variableProjects;
    }

    public Map<ProjectVersionRef, String> getProjectErrors()
    {
        return projectErrors;
    }

    public void setProjectErrors( final Map<ProjectVersionRef, String> projectErrors )
    {
        this.projectErrors = projectErrors;
    }

}
