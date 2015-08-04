package org.commonjava.maven.cartographer.recipe.build;

import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.recipe.MetadataExtractionRecipe;

public class MetadataExtractionRecipeBuilder<T extends MetadataExtractionRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends MetadataExtractionRecipe>
    extends ProjectGraphRecipeBuilder<T, O, R>
{

    public static final class StandaloneMeta
        extends
 ProjectGraphRecipeBuilder<StandaloneMeta, StandaloneRecipeOwner<MetadataExtractionRecipe>, MetadataExtractionRecipe>
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

    public MetadataExtractionRecipeBuilder( final O owner )
    {
        super( owner );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final MetadataExtractionRecipe recipe = new MetadataExtractionRecipe();
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
