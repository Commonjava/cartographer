package org.commonjava.maven.cartographer.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.commonjava.maven.atlas.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

/**
 * Created by jdcasey on 8/7/15.
 */
public class ProjectPathTest
{
    @Test
    public void jsonRoundTrip()
                    throws Exception
    {
        ProjectVersionRef ref = new ProjectVersionRef( "org.foo", "bar", "1" );
        ProjectPath in = new ProjectPath( Collections.singletonList(
                        new ParentRelationship( URI.create( "http://nowhere.com" ), ref,
                                                new ProjectVersionRef( "org.dep", "project", "1.1" ) ) ) );

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules( new ProjectVersionRefSerializerModule(), new ProjectRelationshipSerializerModule() );
        mapper.enable( SerializationFeature.INDENT_OUTPUT );

        String json = mapper.writeValueAsString( in );

        System.out.println(json);

        ProjectPath out = mapper.readValue( json, ProjectPath.class );
    }
}
