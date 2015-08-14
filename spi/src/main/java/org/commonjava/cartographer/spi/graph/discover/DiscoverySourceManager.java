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

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.maven.galley.model.Location;

public interface DiscoverySourceManager
{

    Location createLocation( Object source )
        throws CartoDataException;

    List<? extends Location> createLocations( Object... sources )
        throws CartoDataException;

    List<? extends Location> createLocations( Collection<Object> sources )
        throws CartoDataException;

    URI createSourceURI( String source )
        throws CartoDataException;

    boolean activateWorkspaceSources( ViewParams params, String... sources )
        throws CartoDataException;

    boolean activateWorkspaceSources( ViewParams params, Collection<? extends Location> locations )
        throws CartoDataException;

    String getFormatHint();

}
