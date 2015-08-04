package org.commonjava.maven.cartographer.recipe.build;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRecipe;

public class ProjectGraphRecipeBuilder<T extends ProjectGraphRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends ProjectGraphRecipe>
    extends SingleGraphResolverRecipeBuilder<T, O, R>
{

    public static final class StandaloneProject
        extends
        ProjectGraphRecipeBuilder<StandaloneProject, StandaloneRecipeOwner<ProjectGraphRecipe>, ProjectGraphRecipe>
    {
        public StandaloneProject()
        {
            super( new StandaloneRecipeOwner<>() );
        }

    }

    public static StandaloneProject newProjectGraphRecipeBuilder()
    {
        return new StandaloneProject();
    }

    private GraphDescription graph;

    public ProjectGraphRecipeBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final ProjectGraphRecipe recipe = new ProjectGraphRecipe();
        recipe.setGraph( graph );
        configure( recipe );

        return (R) recipe;
    }

    public T withProjectVersionRef( final ProjectVersionRef ref )
    {
        return withNewGraph().withRoots( ref )
                             .finishGraph();
    }

    @Override
    public GraphDescriptionBuilder<T> withNewGraph()
    {
        return new GraphDescriptionBuilder<>( self );
    }

    @Override
    public T withGraph( final GraphDescription graph )
    {
        this.graph = graph;
        return self;
    }

}
