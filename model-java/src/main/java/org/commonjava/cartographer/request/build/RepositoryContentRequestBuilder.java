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

import org.commonjava.cartographer.request.ExtraCT;
import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.util.*;

public class RepositoryContentRequestBuilder<T extends RepositoryContentRequestBuilder<T, R>, R extends RepositoryContentRequest>
    extends MultiGraphRequestBuilder<T, R>
{

    public static final class StandaloneRCRB
        extends RepositoryContentRequestBuilder<StandaloneRCRB, RepositoryContentRequest>
    {
    }

    public static StandaloneRCRB newRepositoryContentRecipeBuilder()
    {
        return new StandaloneRCRB();
    }

    private boolean multiSourceGAVs;

    private Set<ExtraCT> extras;

    private Set<String> metas;

    private Set<String> excludedSources;

    private transient Set<Location> excludedSourceLocations;

    public Set<String> getExcludedSources()
    {
        return excludedSources;
    }

    public T withExcludedSources( final Set<String> excludedSources )
    {
        this.excludedSources = new TreeSet<String>( excludedSources );
        return self;
    }

    public Set<ExtraCT> getExtras()
    {
        return extras;
    }

    public T withExtras( final Set<ExtraCT> extras )
    {
        this.extras = new TreeSet<>( extras );
        return self;
    }

    public Set<String> getMetas()
    {
        return metas;
    }

    public T withMetas( final Set<String> metas )
    {
        this.metas = metas;
        return self;
    }

    public Set<Location> getExcludedSourceLocations()
    {
        return excludedSourceLocations;
    }

    public T withExcludedSourceLocations( final Set<Location> excludedSourceLocations )
    {
        this.excludedSourceLocations = excludedSourceLocations;
        return self;
    }

    public boolean isMultiSourceGAVs()
    {
        return multiSourceGAVs;
    }

    public T withMultiSourceGAVs( final boolean multiSourceGAVs )
    {
        this.multiSourceGAVs = multiSourceGAVs;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new RepositoryContentRequest();
        configure( recipe );

        return (R) recipe;
    }

    protected void configure( final R recipe )
    {
        recipe.setMultiSourceGAVs( multiSourceGAVs );
        recipe.setExtras( extras );
        recipe.setMetas( metas );
        recipe.setExcludedSourceLocations( excludedSourceLocations );
        recipe.setExcludedSources( excludedSources );
        super.configure( recipe );
    }

    @Override
    public T withGraphs( GraphComposition graphs )
    {
        return super.withGraphs( graphs );
    }

    @Override
    public T withSource( String source )
    {
        return super.withSource( source );
    }

    @Override
    public T withWorkspaceId( String workspaceId )
    {
        return super.withWorkspaceId( workspaceId );
    }

    @Override
    public T withSourceLocation( Location source )
    {
        return super.withSourceLocation( source );
    }

    @Override
    public T withTimeoutSecs( Integer timeoutSecs )
    {
        return super.withTimeoutSecs( timeoutSecs );
    }

    @Override
    public T withPatcherIds( Collection<String> patcherIds )
    {
        return super.withPatcherIds( patcherIds );
    }

    @Override
    public T withResolve( boolean resolve )
    {
        return super.withResolve( resolve );
    }

    @Override
    public T withInjectedBOMs( List<ProjectVersionRef> injectedBOMs )
    {
        return super.withInjectedBOMs( injectedBOMs );
    }

    @Override
    public T withExcludedSubgraphs( Collection<ProjectVersionRef> excludedSubgraphs )
    {
        return super.withExcludedSubgraphs( excludedSubgraphs );
    }

    @Override
    public T withVersionSelections( Map<ProjectRef, ProjectVersionRef> versionSelections )
    {
        return super.withVersionSelections( versionSelections );
    }

}
