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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.Map;

public final class MetadataEntry
{

    private Map<String, String> metadata;

    private ProjectVersionRef project;

    public MetadataEntry()
    {
    }

    public MetadataEntry( ProjectVersionRef project, Map<String, String> metadata )
    {
        this.project = project;
        this.metadata = metadata;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public void setMetadata( Map<String, String> metadata )
    {
        this.metadata = metadata;
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
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        MetadataEntry that = (MetadataEntry) o;

        return !( project != null ? !project.equals( that.project ) : that.project != null );

    }

    @Override
    public int hashCode()
    {
        return project != null ? project.hashCode() : 0;
    }
}
