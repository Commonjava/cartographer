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
package org.commonjava.maven.cartographer.agg;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;

public class ProjectRefCollection
{

    private final Set<ProjectVersionRef> versionRefs = new HashSet<ProjectVersionRef>();

    private final Set<ArtifactRef> artifactRefs = new HashSet<ArtifactRef>();

    public void addVersionRef( final ProjectVersionRef ref )
    {
        versionRefs.add( ref );
    }

    public void addArtifactRef( final ArtifactRef ref )
    {
        addVersionRef( ref.asProjectVersionRef() );
        artifactRefs.add( ref );
    }

    public Set<ProjectVersionRef> getVersionRefs()
    {
        return versionRefs;
    }

    public Set<ArtifactRef> getArtifactRefs()
    {
        return artifactRefs;
    }

    public Set<VersionlessArtifactRef> getVersionlessArtifactRefs()
    {
        final Set<VersionlessArtifactRef> result = new HashSet<VersionlessArtifactRef>();
        for ( final ArtifactRef ref : artifactRefs )
        {
            result.add( new VersionlessArtifactRef( ref ) );
        }

        return result;
    }

}
