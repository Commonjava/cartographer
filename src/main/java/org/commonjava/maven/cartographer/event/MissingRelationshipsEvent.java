package org.commonjava.maven.cartographer.event;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MissingRelationshipsEvent
{

    private final ProjectVersionRef ref;

    private final GraphWorkspace session;

    public MissingRelationshipsEvent( final ProjectVersionRef ref, final GraphWorkspace session )
    {
        this.ref = ref;
        this.session = session;
    }

    public ProjectVersionRef getRef()
    {
        return ref;
    }

    public GraphWorkspace getSession()
    {
        return session;
    }

}
