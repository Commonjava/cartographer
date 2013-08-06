package org.commonjava.maven.cartographer.event;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class NewRelationshipsEvent
{

    private final EProjectDirectRelationships relationships;

    private final GraphWorkspace session;

    public NewRelationshipsEvent( final EProjectDirectRelationships relationships, final GraphWorkspace session )
    {
        this.relationships = relationships;
        this.session = session;
    }

    public final ProjectVersionRef getRef()
    {
        return relationships.getProjectRef();
    }

    public final EProjectDirectRelationships getRelationships()
    {
        return relationships;
    }

    public GraphWorkspace getSession()
    {
        return session;
    }

    public final EProjectKey getKey()
    {
        return relationships.getKey();
    }

}
