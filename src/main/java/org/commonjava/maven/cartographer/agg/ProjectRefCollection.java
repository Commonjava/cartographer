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
