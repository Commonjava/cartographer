package org.commonjava.maven.cartographer.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.cartographer.CartographerCoreBuilder;
import org.commonjava.cartographer.request.build.GraphCompositionBuilder;
import org.commonjava.cartographer.request.build.GraphDescriptionBuilder;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.cartographer.request.build.PomRequestBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PomRecipeTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void jsonRoundTrip_GraphCompOnly()
            throws Exception
    {
        final PomRequest recipe = PomRequestBuilder.newPomRequestBuilder()
                                                   .withGraphs( GraphCompositionBuilder.newGraphCompositionBuilder()
                                                                                       .withGraph(
                                                                                               GraphDescriptionBuilder.newGraphDescriptionBuilder()
                                                                                                                      .withRoots(
                                                                                                                              new ProjectVersionRef(
                                                                                                                                      "org.foo",
                                                                                                                                      "bar",
                                                                                                                                      "1" ) )
                                                                                                                      .build() )
                                                                                       .build() )
                                                   .build();

        final Cartographer carto = new CartographerCoreBuilder( temp.newFolder(),
                                                                new FileNeo4jConnectionFactory( temp.newFolder(),
                                                                                                false ) ).build();

        final ObjectMapper mapper = carto.getObjectMapper();
        logger.debug( "Testing PomRequest serialization with {}", mapper );

        logger.info( "request: {}", recipe );

        final String json = mapper.writeValueAsString( recipe );

        logger.info( "JSON: {}", json );

        final PomRequest result = mapper.readValue( json, PomRequest.class );

        assertThat( result.getGraphComposition(), equalTo( recipe.getGraphComposition() ) );
    }

}
