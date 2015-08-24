/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.cartographer.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.cartographer.CartographerCoreBuilder;
import org.commonjava.cartographer.request.build.GraphCompositionBuilder;
import org.commonjava.cartographer.request.build.GraphDescriptionBuilder;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.cartographer.request.build.PomRequestBuilder;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
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
                                                                                                                              new SimpleProjectVersionRef(
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
