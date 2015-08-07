package org.commonjava.maven.cartographer.recipe.build;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.recipe.PathsRecipe;

public class PathsRecipeBuilder<T extends PathsRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends PathsRecipe>
    extends MultiGraphResolverRecipeBuilder<T, O, R>
{

    protected GraphComposition graphs;

    private Set<ProjectRef> targets;

    public static final class StandalonePaths
        extends PathsRecipeBuilder<StandalonePaths, StandaloneRecipeOwner<PathsRecipe>, PathsRecipe>
    {
        public StandalonePaths()
        {
            super( new StandaloneRecipeOwner<>() );
        }
    }

    public static StandalonePaths newPathsRecipeBuilder()
    {
        return new StandalonePaths();
    }

    public PathsRecipeBuilder( final O owner )
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
        final PathsRecipe recipe = new PathsRecipe();
        configure( recipe );
        configureMultiGraphs( recipe );
        configurePaths( recipe );

        return (R) recipe;
    }

    protected void configurePaths( final PathsRecipe recipe )
    {
        recipe.setTargets( targets );
    }

}
