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
package org.commonjava.maven.cartographer.recipe;

import java.util.Set;
import java.util.TreeSet;

public class MetadataCollationRecipe
    extends ProjectGraphRecipe
{

    private Set<String> keys;

    public Set<String> getKeys()
    {
        return keys;
    }

    public void setKeys( final Set<String> keys )
    {
        this.keys = new TreeSet<>( keys );
    }

    @Override
    public void normalize()
    {
        super.normalize();
        normalize( keys );
    }

}
