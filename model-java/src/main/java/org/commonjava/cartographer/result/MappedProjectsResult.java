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

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdcasey on 8/7/15.
 */
public class MappedProjectsResult
{
    private Set<MappedProjects> projects;

    public boolean addProject( MappedProjects projectMapping )
    {
        if ( projects == null )
        {
            projects = new HashSet<>();
        }

        return projects.add( projectMapping );
    }

    public Set<MappedProjects> getProjects()
    {
        return projects;
    }

    public void setProjects( Set<MappedProjects> projects )
    {
        this.projects = projects;
    }
}

