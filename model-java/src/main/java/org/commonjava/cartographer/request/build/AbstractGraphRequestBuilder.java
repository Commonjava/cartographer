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
package org.commonjava.cartographer.request.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.cartographer.graph.discover.patch.DepgraphPatcherConstants;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.request.AbstractGraphRequest;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

public abstract class AbstractGraphRequestBuilder<T extends AbstractGraphRequestBuilder<T, R>, R extends AbstractGraphRequest>
{

    protected String workspaceId;

    protected Collection<String> patcherIds;

    protected Integer timeoutSecs;

    protected String source;

    protected boolean resolve;

    protected transient Location sourceLocation;

    protected List<ProjectVersionRef> injectedBOMs;

    protected Map<ProjectRef, ProjectVersionRef> versionSelections;

    protected List<ProjectVersionRef> excludedSubgraphs;

    protected final T self;

    @SuppressWarnings( "unchecked" )
    public AbstractGraphRequestBuilder()
    {
        this.patcherIds = DepgraphPatcherConstants.ALL_PATCHERS;
        this.resolve = true;
        this.self = (T) this;
    }

    public abstract R build();

    public String getSource()
    {
        return source;
    }

    public T withSource( final String source )
    {
        this.source = source;
        return self;
    }

    public String getWorkspaceId()
    {
        return workspaceId;
    }

    public Location getSourceLocation()
    {
        return sourceLocation;
    }

    public T withWorkspaceId( final String workspaceId )
    {
        this.workspaceId = workspaceId;
        return self;
    }

    public T withSourceLocation( final Location source )
    {
        this.sourceLocation = source;
        this.source = sourceLocation.getUri();
        return self;
    }

    public Integer getTimeoutSecs()
    {
        return timeoutSecs == null ? 10 : timeoutSecs;
    }

    public T withTimeoutSecs( final Integer timeoutSecs )
    {
        this.timeoutSecs = timeoutSecs;
        return self;
    }

    public Collection<String> getPatcherIds()
    {
        return patcherIds;
    }

    public T withPatcherIds( final Collection<String> patcherIds )
    {
        this.patcherIds = new ArrayList<>();
        for ( final String id : patcherIds )
        {
            if ( !this.patcherIds.contains( id ) )
            {
                this.patcherIds.add( id );
            }
        }
        return self;
    }

    public boolean isResolve()
    {
        return resolve;
    }

    public T withResolve( final boolean resolve )
    {
        this.resolve = resolve;
        return self;
    }

    public List<ProjectVersionRef> getInjectedBOMs()
    {
        return injectedBOMs;
    }

    public T withInjectedBOMs( final List<ProjectVersionRef> injectedBOMs )
    {
        this.injectedBOMs = injectedBOMs;
        return self;
    }

    public List<ProjectVersionRef> getExcludedSubgraphs()
    {
        return excludedSubgraphs;
    }

    public T withExcludedSubgraphs( final Collection<ProjectVersionRef> excludedSubgraphs )
    {
        this.excludedSubgraphs = new ArrayList<ProjectVersionRef>( excludedSubgraphs );
        return self;
    }

    public Map<ProjectRef, ProjectVersionRef> getVersionSelections()
    {
        return versionSelections == null ? new HashMap<ProjectRef, ProjectVersionRef>() : versionSelections;
    }

    public T withVersionSelections( final Map<ProjectRef, ProjectVersionRef> versionSelections )
    {
        this.versionSelections = versionSelections;
        return self;
    }

    protected void configure( final R recipe )
    {
        recipe.setExcludedSubgraphs( excludedSubgraphs );
        recipe.setInjectedBOMs( injectedBOMs );
        recipe.setPatcherIds( patcherIds );
        recipe.setResolve( resolve );
        recipe.setSource( source );
        recipe.setSourceLocation( sourceLocation );
        recipe.setTimeoutSecs( timeoutSecs );
        recipe.setVersionSelections( versionSelections );
        recipe.setWorkspaceId( workspaceId );
    }

}
