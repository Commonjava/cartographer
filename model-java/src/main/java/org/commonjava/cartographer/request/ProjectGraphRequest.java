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
package org.commonjava.cartographer.request;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ProjectGraphRequest
    extends SingleGraphRequest
{

    protected String projectGavPattern;

    protected ProjectVersionRef project;

    public String getProjectGavPattern()
    {
        return projectGavPattern;
    }

    public void setProjectGavPattern( final String projectGavPattern )
    {
        this.projectGavPattern = projectGavPattern;
    }

    public ProjectVersionRef getProject()
    {
        return project;
    }

    public void setProject( final ProjectVersionRef project )
    {
        this.project = project;
    }

}
