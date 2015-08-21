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
package org.commonjava.cartographer.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.commonjava.cartographer.result.ProjectPath;
import org.commonjava.cartographer.result.ProjectPathsResult;
import org.commonjava.maven.atlas.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectPathsResultTest
{

    @Test
    public void jsonRoundTrip()
                    throws Exception
    {
        ProjectPathsResult in = new ProjectPathsResult();
        ProjectVersionRef ref = new ProjectVersionRef( "org.foo", "bar", "1" );
        in.addPath( ref, new ProjectPath( Collections.singletonList(
                        new ParentRelationship( URI.create( "http://nowhere.com" ), ref,
                                                new ProjectVersionRef( "org.dep", "project", "1.1" ) ) ) ) );

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules( new ProjectVersionRefSerializerModule(), new ProjectRelationshipSerializerModule() );
        mapper.enable( SerializationFeature.INDENT_OUTPUT );
        mapper.configure( JsonParser.Feature.ALLOW_COMMENTS, true );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_EMPTY );

        String json = mapper.writeValueAsString( in );

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( json );

        ProjectPathsResult out = mapper.readValue( json, ProjectPathsResult.class );
    }
}
