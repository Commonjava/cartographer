package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MultiGraphRequestBuilder<T extends MultiGraphRequestBuilder<T, R>, R extends MultiGraphRequest>
    extends AbstractGraphRequestBuilder<T, R>
{

    protected GraphComposition graphs;

    public static final class StandaloneMulti
        extends MultiGraphRequestBuilder<StandaloneMulti, MultiGraphRequest>
    {
    }

    public static StandaloneMulti newMultiGraphResolverRequestBuilder()
    {
        return new StandaloneMulti();
    }

    @SuppressWarnings( "unchecked" )
    public T withGraphs( final GraphComposition graphs )
    {
        this.graphs = graphs;
        return (T) this;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new MultiGraphRequest();
        configure( recipe );

        return recipe;
    }

    protected void configure( final R recipe )
    {
        recipe.setGraphComposition( graphs );
        super.configure( recipe );
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
