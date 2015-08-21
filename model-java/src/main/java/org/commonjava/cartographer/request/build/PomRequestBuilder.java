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
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PomRequestBuilder<T extends PomRequestBuilder<T, R>, R extends PomRequest>
    extends RepositoryContentRequestBuilder<T, R>
{

    public static final class StandalonePRB
        extends PomRequestBuilder<StandalonePRB, PomRequest>
    {
    }

    public static StandalonePRB newPomRequestBuilder()
    {
        return new StandalonePRB();
    }

    private boolean generateVersionRanges;

    private ProjectVersionRef output;

    /**
     * Flag saying that all the deps from dependency graph should be placed in
     * the dependencyManagement section. If false standard dependencies section
     * will be used.
     */
    private boolean graphToManagedDeps;

    public boolean isGenerateVersionRanges()
    {
        return generateVersionRanges;
    }

    public T withGenerateVersionRanges( final boolean generateVersionRanges )
    {
        this.generateVersionRanges = generateVersionRanges;
        return self;
    }

    public ProjectVersionRef getOutput()
    {
        return output;
    }

    public T withOutput( final ProjectVersionRef outputGav )
    {
        this.output = outputGav;
        return self;
    }

    /**
     * @return the flag saying that all the deps from dependency graph should be
     *         placed in the dependencyManagement section. If false standard
     *         dependencies section will be used.
     */
    public boolean isGraphToManagedDeps()
    {
        return graphToManagedDeps;
    }

    /**
     * @param graphToManagedDeps
     *            the flag saying that all the deps from dependency graph should
     *            be placed in the dependencyManagement section. If false
     *            standard dependencies section will be used
     */
    public T withGraphToManagedDeps( final boolean graphToManagedDeps )
    {
        this.graphToManagedDeps = graphToManagedDeps;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new PomRequest();
        configure( recipe );

        return recipe;
    }

    protected void configure( final R recipe )
    {
        recipe.setGenerateVersionRanges( generateVersionRanges );
        recipe.setGraphToManagedDeps( graphToManagedDeps );
        recipe.setOutput( output );
        super.configure( recipe );
    }

    @Override
    public T withExcludedSources( Set<String> excludedSources )
    {
        return super.withExcludedSources( excludedSources );
    }

    @Override
    public T withExtras( Set<ExtraCT> extras )
    {
        return super.withExtras( extras );
    }

    @Override
    public T withMetas( Set<String> metas )
    {
        return super.withMetas( metas );
    }

    @Override
    public T withExcludedSourceLocations( Set<Location> excludedSourceLocations )
    {
        return super.withExcludedSourceLocations( excludedSourceLocations );
    }

    @Override
    public T withMultiSourceGAVs( boolean multiSourceGAVs )
    {
        return super.withMultiSourceGAVs( multiSourceGAVs );
    }

    @Override
    public T withLocalUrls( boolean localUrls )
    {
        return super.withLocalUrls( localUrls );
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
