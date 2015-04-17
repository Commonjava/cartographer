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
package org.commonjava.maven.cartographer.dto;

import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class MetadataCollationEntry
{

    private Map<String, String> metadata;

    private Set<ProjectVersionRef> projects;

    public MetadataCollationEntry()
    {
    }

    public MetadataCollationEntry( Map<String, String> metadata, Set<ProjectVersionRef> projects )
    {
        this.metadata = metadata;
        this.projects = projects;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public Set<ProjectVersionRef> getProjects()
    {
        return projects;
    }

    public void setMetadata( Map<String, String> metadata )
    {
        this.metadata = metadata;
    }

    public void setProjects( Set<ProjectVersionRef> projects )
    {
        this.projects = projects;
    }

}
