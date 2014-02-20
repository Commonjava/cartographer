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

import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.util.logging.Logger;

public final class DiscoveryUtils
{

    private static final Logger logger = new Logger( DiscoveryUtils.class );

    private DiscoveryUtils()
    {
    }

    public static void selectSingle( final SingleVersion ver, final ProjectVersionRef ref, final CartoDataManager dataManager )
        throws CartoDataException
    {
        if ( !ver.isConcrete() )
        {
            logger.error( "Cannot select a non-concrete version: %s for: %s. Ignoring.", ver.renderStandard(), ref );
            return;
        }

        try
        {
            GraphWorkspace ws = dataManager.getCurrentWorkspace();
            if ( ws == null )
            {
                ws = dataManager.getGraphManager()
                                .createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
            }

            ws.selectVersion( ref, ref.selectVersion( ver ) );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to record selected version: %s for: %s. Reason: %s", e, ver, ref, e.getMessage() );
        }
    }

    public static void selectWildcard( final SingleVersion ver, final ProjectRef ref, final CartoDataManager dataManager )
        throws CartoDataException
    {
        if ( !ver.isConcrete() )
        {
            logger.error( "Cannot select a non-concrete version: %s for all: %s. Ignoring.", ver.renderStandard(), ref );
            return;
        }

        try
        {
            GraphWorkspace ws = dataManager.getCurrentWorkspace();
            if ( ws == null )
            {
                ws = dataManager.getGraphManager()
                                .createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
            }

            ws.selectVersion( ref, new ProjectVersionRef( ref, ver ) );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to record selected version: %s for all: %s. Reason: %s", e, ver, ref, e.getMessage() );
        }
    }

}
