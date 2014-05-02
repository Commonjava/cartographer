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
package org.commonjava.maven.cartographer.discover;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
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

    DiscoveryResult discoverRelationships( ProjectVersionRef projectId, RelationshipGraph graph,
                                           DiscoveryConfig discoveryConfig )
        throws CartoDataException;

}
