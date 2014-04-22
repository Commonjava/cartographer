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

    boolean activateWorkspaceSources( GraphWorkspace ws, String... sources )
        throws CartoDataException;

    String getFormatHint();

}
