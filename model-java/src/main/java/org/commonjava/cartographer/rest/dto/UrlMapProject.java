/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.rest.dto;

import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class UrlMapProject
{
    private String repoUrl;

    private Set<String> files;

    public UrlMapProject(){}

    public UrlMapProject( final String url, final Set<String> files )
    {
        repoUrl = url;
        this.files = files;
    }

    public String getRepoUrl()
    {
        return repoUrl;
    }

    public void setRepoUrl( final String repoUrl )
    {
        this.repoUrl = repoUrl;
    }

    public Set<String> getFiles()
    {
        return files;
    }

    public void setFiles( final Set<String> files )
    {
        this.files = files;
    }
}
