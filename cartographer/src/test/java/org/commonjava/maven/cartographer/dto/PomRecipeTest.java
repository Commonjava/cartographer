package org.commonjava.maven.cartographer.dto;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.Cartographer;
import org.commonjava.maven.cartographer.CartographerBuilder;
import org.commonjava.maven.cartographer.dto.build.PomRecipeBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PomRecipeTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void jsonRoundTrip_GraphCompOnly()
        throws Exception
    {
        final PomRecipe recipe = PomRecipeBuilder.newPomRecipeBuilder()
                                                 .withNewGraphComposition()
                                                 .withNewGraph()
                                                 .withRoots( new ProjectVersionRef( "org.foo", "bar", "1" ) )
                                                 .finishGraph()
                                                 .finishGraphComposition()
                                                 .build();

        final Cartographer carto =
            new CartographerBuilder( temp.newFolder(), new FileNeo4jConnectionFactory( temp.newFolder(), false ) ).build();

        final ObjectMapper mapper = carto.getObjectMapper();
        logger.debug( "Testing PomRecipe serialization with {}", mapper );

        logger.info( "recipe: {}", recipe );

        final String json = mapper.writeValueAsString( recipe );

        logger.info( "JSON: {}", json );

        final PomRecipe result = mapper.readValue( json, PomRecipe.class );

        assertThat( result.getGraphComposition(), equalTo( recipe.getGraphComposition() ) );
    }

}
