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
package org.commonjava.maven.cartographer.request;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class PomRequest
    extends RepositoryContentRequest
{

    private boolean generateVersionRanges;

    private ProjectVersionRef output;

    /**
     * Flag saying that all the deps from dependency graph should be placed in
     * the dependencyManagement section. If false standard dependencies section
     * will be used.
     */
    private boolean graphToManagedDeps;


    public PomRequest()
    {
    }

    public boolean isGenerateVersionRanges()
    {
        return generateVersionRanges;
    }

    public void setGenerateVersionRanges( final boolean generateVersionRanges )
    {
        this.generateVersionRanges = generateVersionRanges;
    }

    public ProjectVersionRef getOutput()
    {
        return output;
    }

    public void setOutput( final ProjectVersionRef outputGav )
    {
        this.output = outputGav;
    }

    /**
     * @return the flag saying that all the deps from dependency graph should be
     *         placed in the dependencyManagement section. If false standard
     *         dependencies section will be used.
     */
    public boolean isGraphToManagedDeps()
    {
        return graphToManagedDeps;
    }

    /**
     * @param graphToManagedDeps
     *            the flag saying that all the deps from dependency graph should
     *            be placed in the dependencyManagement section. If false
     *            standard dependencies section will be used
     */
    public void setGraphToManagedDeps( boolean graphToManagedDeps )
    {
        this.graphToManagedDeps = graphToManagedDeps;
    }

}
