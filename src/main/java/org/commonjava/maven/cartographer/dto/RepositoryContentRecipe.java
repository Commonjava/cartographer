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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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

    private TreeSet<ExtraCT> extras;

    private TreeSet<String> metas;

    private transient Set<Location> excludedSourceLocations;

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
        return metas == null ? DEFAULT_METAS : metas;
    }

    public void setMetas( final Set<String> metas )
    {
        this.metas = new TreeSet<>( metas );
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
