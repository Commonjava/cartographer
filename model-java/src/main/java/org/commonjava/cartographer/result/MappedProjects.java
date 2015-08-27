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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.List;

/**
 * Created by jdcasey on 8/7/15.
 */
public class MappedProjects
{
    private ProjectVersionRef project;

    private List<ProjectVersionRef> mappedProjects;

    public MappedProjects(){}

    public MappedProjects(ProjectVersionRef project, List<ProjectVersionRef> mappedProjects )
    {
        this.project = project;
        this.mappedProjects = mappedProjects;
    }

    public ProjectVersionRef getProject()
    {
        return project;
    }

    public void setProject( ProjectVersionRef project )
    {
        this.project = project;
    }

    public List<ProjectVersionRef> getMappedProjects()
    {
        return mappedProjects;
    }

    public void setMappedProjects( List<ProjectVersionRef> mappedProjects )
    {
        this.mappedProjects = mappedProjects;
    }

}
