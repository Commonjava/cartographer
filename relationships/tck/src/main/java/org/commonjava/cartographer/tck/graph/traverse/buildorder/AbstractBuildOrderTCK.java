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
package org.commonjava.cartographer.tck.graph.traverse.buildorder;

import static org.apache.commons.lang.StringUtils.join;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.tck.graph.AbstractSPI_TCK;

public class AbstractBuildOrderTCK
    extends AbstractSPI_TCK
{

    protected void assertRelativeOrder( final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder,
                                      final List<ProjectRef> buildOrder )
    {
        for ( final Map.Entry<ProjectVersionRef, ProjectVersionRef> entry : relativeOrder.entrySet() )
        {
            final ProjectRef k = entry.getKey()
                                      .asProjectRef();
            final ProjectRef v = entry.getValue()
                                      .asProjectRef();

            final int kidx = buildOrder.indexOf( k );
            final int vidx = buildOrder.indexOf( v );

            if ( kidx < 0 )
            {
                fail( "Cannot find: " + k + " in build order: " + buildOrder );
            }

            if ( vidx < 0 )
            {
                fail( "Cannot find: " + v + " in build order: " + buildOrder );
            }

            if ( vidx >= kidx )
            {
                fail( "prerequisite project: " + v + " of: " + k + " appears AFTER it in the build order: "
                    + buildOrder );
            }
        }
    }

}
