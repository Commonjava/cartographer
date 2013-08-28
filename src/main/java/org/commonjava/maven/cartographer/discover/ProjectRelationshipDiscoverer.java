package org.commonjava.maven.cartographer.discover;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;

public interface ProjectRelationshipDiscoverer
{

    /**
     * @param ref the variable/non-concrete ref to resolve versions for
     * @return NEVER null; input ref if no changes made; or resolved ref if successful
     */
    ProjectVersionRef resolveSpecificVersion( ProjectVersionRef ref, DiscoveryConfig discoveryConfig )
        throws CartoDataException;

    DiscoveryResult discoverRelationships( ProjectVersionRef projectId, DiscoveryConfig discoveryConfig, boolean store )
        throws CartoDataException;

}
