package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.profileLocation;

import java.net.URI;
import java.util.Set;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;

public class WorkspaceOps
{

    private final EGraphManager graphs;

    private final DiscoverySourceManager sourceFactory;

    public WorkspaceOps( final EGraphManager graphs, final DiscoverySourceManager sourceFactory )
    {
        this.graphs = graphs;
        this.sourceFactory = sourceFactory;
    }

    public boolean delete( final String id )
    {
        return graphs.deleteWorkspace( id );
    }

    public GraphWorkspace createTemporaryWorkspace()
        throws CartoDataException
    {
        return createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
    }

    public GraphWorkspace createTemporaryWorkspace( final GraphWorkspaceConfiguration config )
        throws CartoDataException
    {
        GraphWorkspace workspace;
        try
        {
            workspace = graphs.createTemporaryWorkspace( config );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to initialize session with config: %s. Reason: %s", e, config,
                                          e.getMessage() );
        }

        return workspace;
    }

    public GraphWorkspace create()
        throws CartoDataException
    {
        return create( new GraphWorkspaceConfiguration() );
    }

    public GraphWorkspace create( final GraphWorkspaceConfiguration config )
        throws CartoDataException
    {
        try
        {
            return graphs.createWorkspace( config );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to create workspace with config: %s. Reason: %s", e, config,
                                          e.getMessage() );
        }
    }

    public GraphWorkspace get( final String id )
        throws CartoDataException
    {
        try
        {
            return graphs.getWorkspace( id );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
        }
    }

    public Set<GraphWorkspace> list()
    {
        return graphs.getAllWorkspaces();
    }

    public void addSource( final String source, final GraphWorkspace ws )
        throws CartoDataException
    {
        if ( source != null )
        {
            sourceFactory.activateWorkspaceSources( ws, source );
        }
    }

    public void addProfile( final String profile, final GraphWorkspace ws )
    {
        if ( profile != null )
        {
            final URI pomLocation = profileLocation( profile );
            ws.addActivePomLocation( pomLocation );
        }
    }
}
