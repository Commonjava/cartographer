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

import java.util.HashSet;
import java.util.Set;

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

    private Set<Location> excludedSourceLocations;

    private Set<ExtraCT> extras;

    private Set<String> metas;

    @Override
    public String toString()
    {
        return String.format( "RepositoryContentRecipe [graphs=%s, workspaceId=%s, source-location=%s]", graphComposition, workspaceId,
                              getSourceLocation() );
    }

    public Set<ExtraCT> getExtras()
    {
        return extras;
    }

    public void setExtras( final Set<ExtraCT> extras )
    {
        this.extras = extras;
    }

    public Set<String> getMetas()
    {
        return metas == null ? DEFAULT_METAS : metas;
    }

    public void setMetas( final Set<String> metas )
    {
        this.metas = metas;
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

}
