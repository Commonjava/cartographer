package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphDescription;
import org.commonjava.cartographer.request.MetadataCollationRequest;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.util.*;
import java.util.function.Consumer;

public class MetadataCollationRequestBuilder<T extends MetadataCollationRequestBuilder<T, R>, R extends MetadataCollationRequest>
    extends ProjectGraphRequestBuilder<T, R>
{

    public static final class StandaloneMeta
        extends MetadataCollationRequestBuilder<StandaloneMeta, MetadataCollationRequest>
    {
    }

    public static StandaloneMeta newMetadataRecipeBuilder()
    {
        return new StandaloneMeta();
    }

    private Collection<String> keys;

    public T withKeys( Collection<String> keys )
    {
        this.keys = keys;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new MetadataCollationRequest();
        configure( recipe );

        return recipe;
    }

    protected void configure( R recipe )
    {
        if ( keys != null )
        {
            recipe.setKeys( new HashSet<>( keys ) );
        }
        super.configure( recipe );
    }

    @Override
    public T withProjectVersionRef( ProjectVersionRef ref )
    {
        return super.withProjectVersionRef( ref );
    }

    @Override
    public T withGraph( GraphDescription graph )
    {
        return super.withGraph( graph );
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
