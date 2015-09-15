package org.commonjava.cartographer.embed;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Test;

import javax.inject.Inject;

/**
 * Created by jdcasey on 9/14/15.
 */
public class GraphOpenCloseTest extends AbstractEmbeddableCDIProducerTest
{
    @Inject
    private RelationshipGraphFactory graphFactory;

    @Test
    public void openAndCloseGraph()
            throws Exception
    {
        RelationshipGraph graph =
                graphFactory.open( new ViewParams( "test", new SimpleProjectVersionRef( "group", "artifact", "1" ) ),
                                   true );

        graph.close();
    }
}
