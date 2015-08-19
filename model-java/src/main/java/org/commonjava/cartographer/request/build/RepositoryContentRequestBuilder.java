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

    private boolean localUrls;

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

    public boolean getLocalUrls()
    {
        return localUrls;
    }

    public T withLocalUrls( final boolean localUrls )
    {
        this.localUrls = localUrls;
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
        recipe.setLocalUrls( localUrls );
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
