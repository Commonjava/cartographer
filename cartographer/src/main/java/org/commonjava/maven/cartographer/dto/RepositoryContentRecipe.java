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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.commonjava.maven.galley.model.Location;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RepositoryContentRecipe
    extends MultiGraphResolverRecipe
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

    private static final String NO_METAS = "none";

    private boolean multiSourceGAVs;

    private Set<ExtraCT> extras;

    private Set<String> metas;

    private Set<String> excludedSources;

    @JsonIgnore
    private transient Set<Location> excludedSourceLocations;
    
    private boolean localUrls;

    @Override
    public String toString()
    {
        return String.format( "RepositoryContentRecipe [graphs=%s, workspaceId=%s, source-location=%s]",
                              graphComposition, workspaceId, getSourceLocation() );
    }

    public Set<String> getExcludedSources()
    {
        return excludedSources;
    }

    public void setExcludedSources( final Set<String> excludedSources )
    {
        if ( excludedSources == null )
        {
            return;
        }
        this.excludedSources = new TreeSet<String>( excludedSources );
    }

    public Set<ExtraCT> getExtras()
    {
        return extras;
    }

    public void setExtras( final Set<ExtraCT> extras )
    {
        if ( extras == null )
        {
            return;
        }
        this.extras = new TreeSet<>( extras );
    }

    public Set<String> getMetas()
    {
        if ( metas == null || metas.isEmpty() )
        {
            return DEFAULT_METAS;
        }
        else if ( metas.size() == 1 && metas.contains( NO_METAS ) )
        {
            return Collections.emptySet();
        }
        else
        {
            return metas;
        }
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

    @Override
    public void normalize()
    {
        super.normalize();
        normalize( extras );
        normalize( metas );
        normalize( excludedSourceLocations );
    }

    public boolean getLocalUrls()
    {
        return localUrls;
    }

    public void setLocalUrls( final boolean localUrls )
    {
        this.localUrls = localUrls;
    }

}
