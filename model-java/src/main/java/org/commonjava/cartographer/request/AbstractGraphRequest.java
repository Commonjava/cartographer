/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.commonjava.maven.atlas.graph.filter.ExcludingFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.net.URISyntaxException;
import java.util.*;

public abstract class AbstractGraphRequest
    implements GraphBasedRequest
{

    protected String workspaceId;

    protected List<String> patcherIds;

    protected Integer timeoutSecs;

    protected String source;

    protected boolean resolve;

    @JsonIgnore
    protected transient Location sourceLocation;

    protected List<ProjectVersionRef> injectedBOMs;

    protected Map<ProjectRef, ProjectVersionRef> versionSelections;

    protected List<ProjectVersionRef> excludedSubgraphs;

    @JsonIgnore
    private transient DiscoveryConfig discoveryConfig;

    public String getSource()
    {
        return source;
    }

    public void setSource( final String source )
    {
        this.source = source;
    }

    public String getWorkspaceId()
    {
        return workspaceId;
    }

    public Location getSourceLocation()
    {
        return sourceLocation;
    }

    public void setWorkspaceId( final String workspaceId )
    {
        this.workspaceId = workspaceId;
    }

    public void setSourceLocation( final Location source )
    {
        this.sourceLocation = source;
    }

    public void setDiscoveryConfig( final DiscoveryConfig discoveryConfig )
    {
        this.discoveryConfig = discoveryConfig;
    }

    /**
     * construct a new {@link DiscoveryConfig} with the configured source {@link Location} which should have been set on this instance already.
     * If the source {@link Location} is missing (because {@link AbstractGraphRequest#setSourceLocation(Location)} hasn't been called), throw {@link CartoRequestException}.
     * Throw {@link URISyntaxException} if the location URI is invalid.
     */
    public DiscoveryConfig getDiscoveryConfig()
                    throws CartoRequestException
    {
        return discoveryConfig;
    }

    public Integer getTimeoutSecs()
    {
        return timeoutSecs == null ? 10 : timeoutSecs;
    }

    public void setTimeoutSecs( final Integer timeoutSecs )
    {
        this.timeoutSecs = timeoutSecs;
    }

    public List<String> getPatcherIds()
    {
        return patcherIds;
    }

    public void setPatcherIds( final Collection<String> patcherIds )
    {
        if ( patcherIds == null )
        {
            return;
        }

        this.patcherIds = new ArrayList<>();
        for ( final String id : patcherIds )
        {
            if ( !this.patcherIds.contains( id ) )
            {
                this.patcherIds.add( id );
            }
        }
    }

    public abstract GraphComposition getGraphComposition();

    public boolean isResolve()
    {
        return resolve;
    }

    public void setResolve( final boolean resolve )
    {
        this.resolve = resolve;
    }

    public List<ProjectVersionRef> getInjectedBOMs()
    {
        return injectedBOMs;
    }

    public void setInjectedBOMs( final List<ProjectVersionRef> injectedBOMs )
    {
        this.injectedBOMs = injectedBOMs;
    }

    @JsonIgnore
    public boolean isValid()
    {
        return getWorkspaceId() != null && getSourceLocation() != null && getGraphComposition() != null
            && getGraphComposition().valid();
    }

    public void normalize()
    {
        getGraphComposition().normalize();
        normalize( patcherIds );
    }

    protected void normalize( final Collection<?> coll )
    {
        if ( coll == null )
        {
            return;
        }

        for ( final Iterator<?> it = coll.iterator(); it.hasNext(); )
        {
            if ( it.next() == null )
            {
                it.remove();
            }
        }
    }

    public List<ProjectVersionRef> getExcludedSubgraphs()
    {
        return excludedSubgraphs;
    }

    public void setExcludedSubgraphs( final Collection<ProjectVersionRef> excludedSubgraphs )
    {
        if ( excludedSubgraphs == null )
        {
            return;
        }

        this.excludedSubgraphs = new ArrayList<ProjectVersionRef>( excludedSubgraphs );
    }

    public ProjectRelationshipFilter buildFilter( final ProjectRelationshipFilter filter )
    {
        final List<ProjectVersionRef> excludedSubgraphs = getExcludedSubgraphs();
        if ( excludedSubgraphs == null || excludedSubgraphs.isEmpty() )
        {
            return filter;
        }
        else
        {
            return new ExcludingFilter( excludedSubgraphs, filter );
        }
    }

    public Map<ProjectRef, ProjectVersionRef> getVersionSelections()
    {
        return versionSelections == null ? new HashMap<ProjectRef, ProjectVersionRef>() : versionSelections;
    }

    public void setVersionSelections( final Map<ProjectRef, ProjectVersionRef> versionSelections )
    {
        this.versionSelections = versionSelections;
    }

    @JsonIgnore
    public void setDefaultPreset( final String defaultPreset )
    {
        getGraphComposition().setDefaultPreset( defaultPreset );
    }


}
