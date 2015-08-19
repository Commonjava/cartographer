package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.util.*;
import java.util.function.Consumer;

public class PathsRequestBuilder<T extends PathsRequestBuilder<T, R>, R extends PathsRequest>
    extends MultiGraphRequestBuilder<T, R>
{

    public static final class StandalonePaths
        extends PathsRequestBuilder<StandalonePaths, PathsRequest>
    {
    }

    public static StandalonePaths newPathsRecipeBuilder()
    {
        return new StandalonePaths();
    }

    private Set<ProjectRef> targets;

    public T withTargets( final Collection<ProjectRef> targets )
    {
        this.targets = targets instanceof Set ? (Set<ProjectRef>) targets : new HashSet<>( targets );
        return self;
    }

    public T withTargets( final ProjectRef... targets )
    {
        this.targets = new HashSet<>( Arrays.asList( targets ) );
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new PathsRequest();
        configure( recipe );

        return recipe;
    }

    protected void configure( final R recipe )
    {
        recipe.setTargets( targets );
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
