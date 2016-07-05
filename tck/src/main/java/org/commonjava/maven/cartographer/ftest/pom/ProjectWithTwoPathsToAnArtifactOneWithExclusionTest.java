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
package org.commonjava.maven.cartographer.ftest.pom;

import org.apache.maven.model.Model;
import org.commonjava.cartographer.graph.preset.ScopeWithEmbeddedProjectsFilter;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.maven.cartographer.ftest.AbstractCartographerTCK;
import org.junit.Test;


/**
 * TCK test class checking that a parent of a transitive dependency accessible through 2 paths, one of them with
 * declared exclusion of the transitive dependency in dependency management, is included when running generatečPOM()
 * method. The dependency graph looks like this:
 * <pre>
 *   +----------+
 *   | consumer |
 *   +----------+
 *        |
 *        | depends on
 *        V
 *   +---------+ dapMgmt specifies transitive-dep with exclusion of excluded-dep
 *   |   dep   |-----------------------+
 *   +---------+                       | depends on
 *        |                            V
 *        |                      +-----------+
 *        | depends on           |   dep2    |
 *        |                      +-----------+
 *        V                            |
 * +----------------+                  | depends on
 * | transitive-dep |<-----------------+
 * +----------------+
 *        |
 *        | depends on
 *        V
 *  +--------------+
 *  | excluded-dep |
 *  +--------------+
 *        |
 *        | has parent
 *        V
 *   +----------+
 *   |  parent  |
 *   +----------+
 * </pre>
 *
 * The {@code consumer} is used as the request root artifact. Used preset is "requires", which results in usage of
 * {@link ScopeWithEmbeddedProjectsFilter} with scope runtime, i.e. runtime dependency graph. Consumer pom, jars of dep,
 * dep2, dep3 and dep4 and parent pom are expected to be in the result.
 *
 * The motivation for this test was an artifact embedding EAP assembly. The assembly specified dependency management of
 * jbossxts with exclusion of arjunacore. The problem is when the EAP assembly is not the root artifact, the dependency
 * management got used only for the direct path while jbossxts is referenced also transitively through jboss-as-xts and
 * jbosstxbridge, for which the exclusion is not specified. The graph aggregator marked the jbossxts as seen while it
 * didn't store arjunacore's relationships, because the rel was not accepted. So as the result arjunacore-all, which is
 * arjunacore's parent was missing in the result, but arjunacore itself was included, which was correct.
 */
public class ProjectWithTwoPathsToAnArtifactOneWithExclusionTest
        extends AbstractCartographerTCK
{

    private static final String PROJECT = "two-paths-one-with-exclusion";

    @Test
    public void run()
        throws Exception
    {
        final String dto = "pom.json";
        final String depsTxt = "deps.txt";
        final String repoResource = "/repo/org/foo/consumer/1/consumer-1.pom";
        final int repoResourceTrim = 5;
        final String alias = "test";

        aliasRepo( alias, repoResource, repoResourceTrim );

        final PomRequest recipe = readRecipe( dto, PomRequest.class );

        final Model pom = carto.getRenderer()
                               .generatePOM( recipe );

        assertPomDeps( pom, false, depsTxt );
    }

    @Override
    protected String getTestDir()
    {
        return PROJECT;
    }

}
