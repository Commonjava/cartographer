/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.cartographer.dto;

import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class MetadataCollationEntry
{
    
    private Map<String, String> metadata;
    
    private Set<ProjectVersionRef> projects;
    
    public MetadataCollationEntry(){}

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
