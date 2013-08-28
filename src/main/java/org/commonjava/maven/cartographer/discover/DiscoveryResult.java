package org.commonjava.maven.cartographer.discover;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class DiscoveryResult
{

    private final ProjectVersionRef selected;

    private final Set<ProjectRelationship<?>> rejected;

    private final Set<ProjectRelationship<?>> discovered;

    private transient Set<ProjectRelationship<?>> accepted;

    public DiscoveryResult( final ProjectVersionRef selected, final Set<ProjectRelationship<?>> discovered )
    {
        this( selected, discovered, Collections.<ProjectRelationship<?>> emptySet() );
    }

    public DiscoveryResult( final ProjectVersionRef selected, final Set<ProjectRelationship<?>> discovered, final Set<ProjectRelationship<?>> rejected )
    {
        this.selected = selected;
        this.discovered = discovered;
        this.rejected = rejected;
    }

    public DiscoveryResult( final DiscoveryResult original, final Set<ProjectRelationship<?>> newlyRejected )
    {
        this.selected = original.getSelectedRef();
        this.discovered = original.getAllDiscoveredRelationships();

        this.rejected = new HashSet<>();
        rejected.addAll( original.getRejectedRelationships() );
        rejected.addAll( newlyRejected );
    }

    public ProjectVersionRef getSelectedRef()
    {
        return selected;
    }

    public Set<ProjectRelationship<?>> getRejectedRelationships()
    {
        return rejected;
    }

    public Set<ProjectRelationship<?>> getAllDiscoveredRelationships()
    {
        return discovered;
    }

    public Set<ProjectRelationship<?>> getAcceptedRelationships()
    {
        if ( discovered == null )
        {
            return null;
        }

        if ( accepted == null )
        {
            accepted = new HashSet<>( discovered );
            accepted.removeAll( rejected );
        }

        return accepted;
    }

}
