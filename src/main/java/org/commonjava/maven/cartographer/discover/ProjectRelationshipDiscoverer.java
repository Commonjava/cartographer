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

    /**
     * @deprecated Use {@link #discoverRelationships(ProjectVersionRef,DiscoveryConfig)} instead
     */
    DiscoveryResult discoverRelationships( ProjectVersionRef projectId, DiscoveryConfig discoveryConfig, boolean store )
        throws CartoDataException;

    DiscoveryResult discoverRelationships( ProjectVersionRef projectId, DiscoveryConfig discoveryConfig )
        throws CartoDataException;

}
