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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

public class RepositoryContentRecipe
    extends ResolverRecipe
{
    private static final Set<String> DEFAULT_METAS = new HashSet<String>()
    {
        private static final long serialVersionUID = 1L;

        {
            add( "sha1" );
            add( "md5" );
            add( "asc" );
        }
    };

    private boolean multiSourceGAVs;

    private Set<ExtraCT> extras;

    private Set<String> metas;

    private transient Set<Location> excludedSourceLocations;
    
    private List<ProjectVersionRef> injectedBOMs;

    @Override
    public String toString()
    {
        return String.format( "RepositoryContentRecipe [graphs=%s, workspaceId=%s, source-location=%s]",
                              graphComposition, workspaceId, getSourceLocation() );
    }

    public Set<ExtraCT> getExtras()
    {
        return extras;
    }

    public void setExtras( final Set<ExtraCT> extras )
    {
        this.extras = new TreeSet<>( extras );
    }

    public Set<String> getMetas()
    {
        return metas == null || metas.isEmpty() ? DEFAULT_METAS : metas;
    }

    public void setMetas( final Set<String> metas )
    {
        this.metas = metas == null || metas.isEmpty() ? null : new TreeSet<>( metas );
    }

    public Set<Location> getExcludedSourceLocations()
    {
        return excludedSourceLocations;
    }

    public void setExcludedSourceLocations( final Set<Location> excludedSourceLocations )
    {
        this.excludedSourceLocations = excludedSourceLocations;
    }

    public boolean hasWildcardExtras()
    {
        if ( extras != null )
        {
            for ( final ExtraCT extra : extras )
            {
                if ( ExtraCT.WILDCARD.equals( extra.getClassifier() ) || ExtraCT.WILDCARD.equals( extra.getType() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isMultiSourceGAVs()
    {
        return multiSourceGAVs;
    }

    public void setMultiSourceGAVs( final boolean multiSourceGAVs )
    {
        this.multiSourceGAVs = multiSourceGAVs;
    }

    public List<ProjectVersionRef> getInjectedBOMs()
    {
        return injectedBOMs;
    }

    public void setInjectedBOMs( final List<ProjectVersionRef> injectedBOMs )
    {
        this.injectedBOMs = injectedBOMs;
    }

    @Override
    public void normalize()
    {
        super.normalize();
        normalize( extras );
        normalize( metas );
        normalize( excludedSourceLocations );
    }

}
