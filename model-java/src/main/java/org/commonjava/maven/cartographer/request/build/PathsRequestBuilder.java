package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.cartographer.request.GraphComposition;
import org.commonjava.maven.cartographer.request.PathsRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PathsRequestBuilder<T extends PathsRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends PathsRequest>
    extends MultiGraphRequestBuilder<T, O, R>
{

    protected GraphComposition graphs;

    private Set<ProjectRef> targets;

    public static final class StandalonePaths
        extends PathsRequestBuilder<StandalonePaths, StandaloneRequestOwner<PathsRequest>, PathsRequest>
    {
        public StandalonePaths()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandalonePaths newPathsRecipeBuilder()
    {
        return new StandalonePaths();
    }

    public PathsRequestBuilder( final O owner )
    {
        super( owner );
    }

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
        final PathsRequest recipe = new PathsRequest();
        configure( recipe );
        configureMultiGraphs( recipe );
        configurePaths( recipe );

        return (R) recipe;
    }

    protected void configurePaths( final PathsRequest recipe )
    {
        recipe.setTargets( targets );
    }

}
