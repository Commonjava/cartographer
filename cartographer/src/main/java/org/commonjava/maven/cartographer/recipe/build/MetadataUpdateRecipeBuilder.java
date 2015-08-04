package org.commonjava.maven.cartographer.recipe.build;

import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.recipe.MetadataUpdateRecipe;

public class MetadataUpdateRecipeBuilder<T extends MetadataUpdateRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends MetadataUpdateRecipe>
    extends ProjectGraphRecipeBuilder<T, O, R>
{

    public static final class StandaloneMeta
        extends
        ProjectGraphRecipeBuilder<StandaloneMeta, StandaloneRecipeOwner<MetadataUpdateRecipe>, MetadataUpdateRecipe>
    {
        public StandaloneMeta()
        {
            super( new StandaloneRecipeOwner<>() );
        }
    }

    public static StandaloneMeta newMetadataRecipeBuilder()
    {
        return new StandaloneMeta();
    }

    private GraphDescription graph;

    public MetadataUpdateRecipeBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MetadataUpdateRecipe recipe = new MetadataUpdateRecipe();
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
