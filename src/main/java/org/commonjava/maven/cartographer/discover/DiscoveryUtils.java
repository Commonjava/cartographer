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

            ws.selectVersionForAll( ref, new ProjectVersionRef( ref, ver ) );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to record selected version: %s for all: %s. Reason: %s", e, ver, ref, e.getMessage() );
        }
    }

}
