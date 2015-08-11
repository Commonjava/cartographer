package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphDescription;
import org.commonjava.maven.cartographer.request.ProjectGraphRelationshipsRequest;

public class ProjectGraphRelationshipsRequestBuilder<T extends ProjectGraphRelationshipsRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends ProjectGraphRelationshipsRequest>
    extends ProjectGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneProjectRels
        extends ProjectGraphRequestBuilder<StandaloneProjectRels, StandaloneRequestOwner<ProjectGraphRelationshipsRequest>, ProjectGraphRelationshipsRequest>
    {
        public StandaloneProjectRels()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandaloneProjectRels newProjectGraphRelationshipsRecipeBuilder()
    {
        return new StandaloneProjectRels();
    }

    private GraphDescription graph;

    public ProjectGraphRelationshipsRequestBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final ProjectGraphRelationshipsRequest recipe = new ProjectGraphRelationshipsRequest();
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
