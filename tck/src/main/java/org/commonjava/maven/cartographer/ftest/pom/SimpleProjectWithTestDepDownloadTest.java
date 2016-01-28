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
 * TCK test class checking that a simple test-scoped dependency of a project is included when running generatečPOM()
 * method. The dependency graph looks like this:
 * <pre>
 *   +----------+
 *   | consumer |
 *   +----------+
 *        |
 *        | depends on (test scope)
 *        V
 *   +----------+
 *   |   dep    |
 *   +----------+
 * </pre>
 *
 * The {@code consumer} is used as the request root artifact. Used preset is "build-requires", which results in usage of
 * {@link org.commonjava.cartographer.graph.preset.BuildRequirementProjectsFilter}, i.e. build-time dependency graph.
 * Consumer pom and dep jar are expected to be in the result.
 */
public class SimpleProjectWithTestDepDownloadTest
        extends AbstractCartographerTCK
{

    private static final String PROJECT = "simple-test-dep";

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
