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
package org.commonjava.cartographer.tck.graph.cycle;

import org.commonjava.cartographer.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.cartographer.tck.graph.AbstractSPI_TCK;
import org.junit.Test;

import java.net.URI;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CycleDetection_IntroduceToExistingTCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef dep = new SimpleProjectVersionRef( "org.other", "dep", "1.0" );
        final ProjectVersionRef dep2 = new SimpleProjectVersionRef( "org.other", "dep2", "1.0" );

        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new SimpleDependencyRelationship( source, project, new SimpleArtifactRef( dep, null, null ), null, 0, false, false, false ),
                                         new SimpleDependencyRelationship( source, dep,  new SimpleArtifactRef( dep2,  null, null ), null, 0, false, false, false ) );

        final boolean introduces = graph.introducesCycle( new SimpleDependencyRelationship( source, dep,  new SimpleArtifactRef( project,  null, null ), null, 0, false, false, false ) );
        /* @formatter:on */

        assertThat( introduces, equalTo( true ) );
    }

}
