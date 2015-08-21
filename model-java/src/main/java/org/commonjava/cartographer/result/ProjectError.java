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
import org.commonjava.maven.atlas.ident.util.ProjectVersionRefComparator;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectError
    implements Comparable<ProjectError>
{
    private ProjectVersionRef project;
    private String error;

    public ProjectError(){}

    public ProjectError( ProjectVersionRef project, String error )
    {
        this.project = project;
        this.error = error;
    }

    public String getError()
    {
        return error;
    }

    public void setError( String error )
    {
        this.error = error;
    }

    public ProjectVersionRef getProject()
    {

        return project;
    }

    public void setProject( ProjectVersionRef project )
    {
        this.project = project;
    }

    @Override
    public int compareTo( ProjectError o )
    {
        return new ProjectVersionRefComparator().compare( project, o.getProject() );
    }
}
