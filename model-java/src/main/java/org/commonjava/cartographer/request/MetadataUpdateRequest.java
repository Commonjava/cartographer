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
package org.commonjava.cartographer.request;

import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MetadataUpdateRequest
    extends ProjectGraphRequest
{

    private Map<String, String> globalMetadata;

    private Map<ProjectVersionRef, Map<String, String>> projectMetadata;

    public Map<String, String> getGlobalMetadata()
    {
        return globalMetadata;
    }

    public void setGlobalMetadata( final Map<String, String> globalMetadata )
    {
        this.globalMetadata = globalMetadata;
    }

    public Map<ProjectVersionRef, Map<String, String>> getProjectMetadata()
    {
        return projectMetadata;
    }

    public void setProjectMetadata( final Map<ProjectVersionRef, Map<String, String>> projectMetadata )
    {
        this.projectMetadata = projectMetadata;
    }

}
