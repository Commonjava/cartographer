/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
