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

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.galley.model.Location;

public interface DiscoverySourceManager
{

    Location createLocation( Object source );

    List<? extends Location> createLocations( Object... sources );

    List<? extends Location> createLocations( Collection<Object> sources );

    URI createSourceURI( String source );

    void activateWorkspaceSources( GraphWorkspace ws, String... sources )
        throws CartoDataException;

    String getFormatHint();

}
