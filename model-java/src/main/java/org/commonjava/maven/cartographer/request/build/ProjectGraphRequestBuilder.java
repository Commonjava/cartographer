package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.request.GraphDescription;
import org.commonjava.maven.cartographer.request.ProjectGraphRequest;

public class ProjectGraphRequestBuilder<T extends ProjectGraphRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends ProjectGraphRequest>
    extends SingleGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneProject
        extends ProjectGraphRequestBuilder<StandaloneProject, StandaloneRequestOwner<ProjectGraphRequest>, ProjectGraphRequest>
    {
        public StandaloneProject()
        {
            super( new StandaloneRequestOwner<>() );
        }

    }

    public static StandaloneProject newProjectGraphRecipeBuilder()
    {
        return new StandaloneProject();
    }

    private GraphDescription graph;

    public ProjectGraphRequestBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final ProjectGraphRequest recipe = new ProjectGraphRequest();
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
