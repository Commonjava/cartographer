package org.commonjava.maven.cartographer.recipe.build;

import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRelationshipsRecipe;

public class ProjectGraphRelationshipsRecipeBuilder<T extends ProjectGraphRelationshipsRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends ProjectGraphRelationshipsRecipe>
    extends ProjectGraphRecipeBuilder<T, O, R>
{

    public static final class StandaloneProjectRels
        extends
        ProjectGraphRecipeBuilder<StandaloneProjectRels, StandaloneRecipeOwner<ProjectGraphRelationshipsRecipe>, ProjectGraphRelationshipsRecipe>
    {
        public StandaloneProjectRels()
        {
            super( new StandaloneRecipeOwner<>() );
        }
    }

    public static StandaloneProjectRels newProjectGraphRelationshipsRecipeBuilder()
    {
        return new StandaloneProjectRels();
    }

    private GraphDescription graph;

    public ProjectGraphRelationshipsRecipeBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final ProjectGraphRelationshipsRecipe recipe = new ProjectGraphRelationshipsRecipe();
        recipe.setGraph( graph );
        configure( recipe );

        return (R) recipe;
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
