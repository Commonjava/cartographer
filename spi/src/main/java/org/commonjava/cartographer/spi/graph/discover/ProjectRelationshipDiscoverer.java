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
package org.commonjava.cartographer.spi.graph.discover;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

public interface ProjectRelationshipDiscoverer
{

    /**
     * @param ref the variable/non-concrete ref to graph versions for
     * @return NEVER null; input ref if no changes made; or resolved ref if successful
     */
    ProjectVersionRef resolveSpecificVersion( ProjectVersionRef ref, DiscoveryConfig discoveryConfig )
        throws CartoDataException;

    DiscoveryResult discoverRelationships( ProjectVersionRef projectId, RelationshipGraph graph,
                                           DiscoveryConfig discoveryConfig )
        throws CartoDataException;

}
