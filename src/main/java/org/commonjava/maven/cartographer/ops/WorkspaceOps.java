package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.profileLocation;

import java.net.URI;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;

@ApplicationScoped
public class WorkspaceOps
{

    @Inject
    private CartoDataManager data;

    @Inject
    private DiscoverySourceManager sourceFactory;

    protected WorkspaceOps()
    {
    }

    public WorkspaceOps( final CartoDataManager data, final DiscoverySourceManager sourceFactory )
    {
        this.data = data;
        this.sourceFactory = sourceFactory;
    }

    public boolean delete( final String id )
    {
        return data.deleteWorkspace( id );
    }

    public GraphWorkspace createTemporaryWorkspace()
        throws CartoDataException
    {
        return createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
    }

    public GraphWorkspace createTemporaryWorkspace( final GraphWorkspaceConfiguration config )
        throws CartoDataException
    {
        return data.createTemporaryWorkspace( config );
    }

    public GraphWorkspace create()
        throws CartoDataException
    {
        return create( new GraphWorkspaceConfiguration() );
    }

    public GraphWorkspace create( final GraphWorkspaceConfiguration config )
        throws CartoDataException
    {
        return data.createWorkspace( config );
    }

    public GraphWorkspace get( final String id )
        throws CartoDataException
    {
        return data.getWorkspace( id );
    }

    public Set<GraphWorkspace> list()
    {
        return data.getAllWorkspaces();
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
