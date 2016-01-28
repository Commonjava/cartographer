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
package org.commonjava.maven.cartographer.ftest.content;

import org.commonjava.cartographer.graph.preset.ScopeWithEmbeddedProjectsFilter;
import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.cartographer.result.ProjectPathsResult;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.ftest.AbstractCartographerTCK;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.junit.Test;

import java.util.Map;

/**
 * TCK test class checking that a simple dependency of a project is included when running getPaths() method. The
 * dependency graph looks like this:
 * <pre>
 *   +----------+
 *   | consumer |
 *   +----------+
 *        |
 *        | depends on
 *        V
 *   +----------+
 *   |   dep    |
 *   +----------+
 * </pre>
 *
 * The {@code consumer} is used as the request root artifact. Used preset is "requires", which results in usage of
 * {@link ScopeWithEmbeddedProjectsFilter} with scope runtime, i.e. runtime dependency graph. Dep jar is expected to be
 * in the result.
 */
public class SimpleProjectWithOneDepDownloadTest
        extends AbstractCartographerTCK
{

    private static final String PROJECT = "simple-dep";

    @Test
    public void run()
        throws Exception
    {
        final String dto = "content.json";
        final String depsTxt = "deps.txt";
        final String repoResource = "/repo/org/foo/consumer/1/consumer-1.pom";
        final int repoResourceTrim = 5;
        final String alias = "test";

        aliasRepo( alias, repoResource, repoResourceTrim );

        RepositoryContentRequest recipe = readRecipe( dto, RepositoryContentRequest.class );

        Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contentMap =
                carto.getResolver().resolveRepositoryContents( recipe );

        System.out.println( contentMap );
    }

    @Override
    protected String getTestDir()
    {
        return PROJECT;
    }

}
