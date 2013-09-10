package org.commonjava.maven.cartographer.util.maven;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.model.Location;

@ApplicationScoped
public class MavenPomReader
{

    @Inject
    private ArtifactManager artifacts;

    public MavenPomReader()
    {
    }

    public MavenPomView read( final ProjectVersionRef ref, final Location... locations )
        throws MavenPomException
    {

        return null;
    }

}
