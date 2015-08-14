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
package org.commonjava.cartographer.spi.event;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.CartoDataException;

public interface CartoEventManager
{

    /*
     * In case we get here before the thread calling waitFor(), we'll inject a new
     * lock, unlock it, and wait a little while for the waitFor() method to run,
     * before removing it and exiting.
     */
    void notifyOfGraph( ProjectVersionRef ref );

    void waitForGraph( ProjectVersionRef ref, RelationshipGraph graph, long timeoutMillis )
            throws CartoDataException;

    void fireErrorEvent( final ProjectRelationshipsErrorEvent evt );

    void fireStorageEvent( final RelationshipStorageEvent evt );

}
