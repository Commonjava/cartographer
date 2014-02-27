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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;

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
        this.discovered = discovered == null ? null : new HashSet<ProjectRelationship<?>>( discovered );
        this.rejected = rejected;
    }

    public DiscoveryResult( final URI source, final DiscoveryResult original, final Set<ProjectRelationship<?>> newlyRejected )
    {
        this.source = source;
        this.selected = original.getSelectedRef();
        this.discovered = original.getAllDiscoveredRelationships();
        if ( this.discovered != null )
        {
            this.discovered = new HashSet<ProjectRelationship<?>>( this.discovered );
        }

        this.rejected = new HashSet<ProjectRelationship<?>>();
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
            accepted = new HashSet<ProjectRelationship<?>>( discovered );
            accepted.removeAll( rejected );
        }

        return accepted;
    }

    @Override
    public synchronized String toString()
    {
        return String.format( "DiscoveryResult [selected=%s]\n  %s", selected,
                              discovered == null ? "-NONE-" : new JoinString( "\n  ", new HashSet<ProjectRelationship<?>>( discovered ) ) );
    }

    public synchronized boolean removeDiscoveredRelationship( final ProjectRelationship<?> rel )
    {
        final boolean result = discovered.remove( rel );
        accepted = null;

        return result;
    }

    public synchronized boolean addDiscoveredRelationship( final ProjectRelationship<?> rel )
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
