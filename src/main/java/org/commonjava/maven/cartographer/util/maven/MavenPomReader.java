package org.commonjava.maven.cartographer.util.maven;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.ArtifactMetadataManager;

@ApplicationScoped
public class MavenPomReader
{

    @Inject
    private ArtifactManager artifactManager;

    @Inject
    private ArtifactMetadataManager metadataManager;

    public MavenPomReader()
    {
    }

    public MavenPomReader( final ArtifactManager artifactManager, final ArtifactMetadataManager metadataManager )
    {
        this.artifactManager = artifactManager;
        this.metadataManager = metadataManager;
    }

    public MavenPomView read( ProjectVersionRef ref )
        throws MavenPomException
    {
        if ( !ref.isSpecificVersion() )
        {
            throw new MavenPomException( "Cannot read POM for '%s'. It does not reference any single GAV (even a snaphot).", ref );
        }

        if ( !ref.isRelease() )
        {
            ref = artifactManager.resolveSnapshotVersion( ref );
        }

        return null;
    }

}
