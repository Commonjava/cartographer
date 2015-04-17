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
package org.commonjava.maven.cartographer.preset;

import java.util.Map;

import org.commonjava.maven.atlas.ident.DependencyScope;

public final class CommonPresetParameters
{

    public static final String SCOPE = "scope";

    public static final String MANAGED = "managed";

    private CommonPresetParameters()
    {
    }

    public static void coerce( final Map<String, Object> params )
    {
        Object scope = params.remove( SCOPE );
        if ( scope != null )
        {
            if ( !( scope instanceof DependencyScope ) )
            {
                scope = DependencyScope.getScope( scope.toString() );
            }

            params.put( SCOPE, scope );
        }

        Object managed = params.remove( MANAGED );
        if ( managed != null )
        {
            if ( !( managed instanceof Boolean ) )
            {
                managed = Boolean.valueOf( managed.toString() );
            }

            params.put( MANAGED, managed );
        }
    }

}
