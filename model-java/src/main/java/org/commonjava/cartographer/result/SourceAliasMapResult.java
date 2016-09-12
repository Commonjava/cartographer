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
package org.commonjava.cartographer.result;

import java.util.Map;

/**
 * Created by jdcasey on 9/9/16.
 */
public class SourceAliasMapResult
{
    private Map<String, String> aliases;

    public SourceAliasMapResult(){}

    public SourceAliasMapResult( Map<String, String> aliases )
    {
        this.aliases = aliases;
    }

    public Map<String, String> getAliases()
    {
        return aliases;
    }

    public void setAliases( Map<String, String> aliases )
    {
        this.aliases = aliases;
    }
}
