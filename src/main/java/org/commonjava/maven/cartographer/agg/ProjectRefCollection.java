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
