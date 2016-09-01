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
package org.commonjava.cartographer.embed;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by jdcasey on 9/14/15.
 */
@ApplicationScoped
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
