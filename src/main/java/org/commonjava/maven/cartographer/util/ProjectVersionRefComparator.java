package org.commonjava.maven.cartographer.util;

import java.util.Comparator;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

public class ProjectVersionRefComparator
    implements Comparator<ProjectVersionRef>
{

    @Override
    public int compare( final ProjectVersionRef first, final ProjectVersionRef second )
    {
        int comp = first.getGroupId()
                        .compareTo( second.getGroupId() );

        if ( comp == 0 )
        {
            comp = first.getArtifactId()
                        .compareTo( second.getArtifactId() );
        }

        if ( comp == 0 )
        {
            try
            {
                comp = first.getVersionSpec()
                            .compareTo( second.getVersionSpec() );
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                comp = first.getVersionString()
                            .compareTo( second.getVersionString() );
            }
        }

        return comp;
    }

}
