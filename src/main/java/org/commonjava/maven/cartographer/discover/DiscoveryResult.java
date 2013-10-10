package org.commonjava.maven.cartographer.discover;

import static org.apache.commons.lang.StringUtils.join;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class DiscoveryResult
{

    private final ProjectVersionRef selected;

    private final URI source;

    private Set<ProjectRelationship<?>> rejected;

    private Set<ProjectRelationship<?>> discovered;

    private transient Set<ProjectRelationship<?>> accepted;

    private Map<String, String> metadata;

    public DiscoveryResult( final URI source, final ProjectVersionRef selected, final Set<ProjectRelationship<?>> discovered )
    {
        this( source, selected, discovered, Collections.<ProjectRelationship<?>> emptySet() );
    }

    public DiscoveryResult( final URI source, final ProjectVersionRef selected, final Set<ProjectRelationship<?>> discovered,
                            final Set<ProjectRelationship<?>> rejected )
    {
        this.source = source;
        this.selected = selected;
        this.discovered = discovered;
        this.rejected = rejected;
    }

    public DiscoveryResult( final URI source, final DiscoveryResult original, final Set<ProjectRelationship<?>> newlyRejected )
    {
        this.source = source;
        this.selected = original.getSelectedRef();
        this.discovered = original.getAllDiscoveredRelationships();

        this.rejected = new HashSet<>();
        rejected.addAll( original.getRejectedRelationships() );
        rejected.addAll( newlyRejected );
    }

    public URI getSource()
    {
        return source;
    }

    public void setRejectedRelationships( final Set<ProjectRelationship<?>> rejected )
    {
        this.rejected = rejected;
        this.accepted = null;
    }

    public void setDiscoveredRelationships( final Set<ProjectRelationship<?>> discovered )
    {
        this.discovered = discovered;
        this.accepted = null;
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

    @Override
    public String toString()
    {
        return String.format( "DiscoveryResult [selected=%s]\n  %s", selected, discovered == null ? "-NONE-" : join( discovered, "\n  " ) );
    }

    public boolean removeDiscoveredRelationship( final ProjectRelationship<?> rel )
    {
        final boolean result = discovered.remove( rel );
        accepted = null;

        return result;
    }

    public boolean addDiscoveredRelationship( final ProjectRelationship<?> rel )
    {
        final boolean result = discovered.add( rel );
        accepted = null;

        return result;
    }

    public boolean removeRejectedRelationship( final ProjectRelationship<?> rel )
    {
        final boolean result = rejected.remove( rel );
        accepted = null;

        return result;
    }

    public boolean addRejectedRelationship( final ProjectRelationship<?> rel )
    {
        final boolean result = rejected.add( rel );
        accepted = null;

        return result;
    }

    public void setMetadata( final Map<String, String> metadata )
    {
        this.metadata = metadata;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

}
