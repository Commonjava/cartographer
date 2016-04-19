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
package org.commonjava.cartographer.rest.util;

import org.commonjava.cartographer.graph.preset.CommonPresetParameters;
import org.commonjava.maven.atlas.ident.DependencyScope;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class PresetParamParser
{

    public Map<String, Object> parse( final Map<String, String[]> params )
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        String[] vals = params.get( CommonPresetParameters.SCOPE );
        if ( vals == null || vals.length < 1 )
        {
            vals = params.get( "s" );
        }

        if ( vals != null && vals.length > 0 )
        {
            result.put( CommonPresetParameters.SCOPE, DependencyScope.getScope( vals[0] ) );
        }

        vals = params.get( CommonPresetParameters.MANAGED );
        if ( vals == null || vals.length < 1 )
        {
            vals = params.get( "m" );
        }

        if ( vals != null && vals.length > 0 )
        {
            result.put( CommonPresetParameters.MANAGED, Boolean.valueOf( vals[0] ) );
        }

        return result;
    }

}
