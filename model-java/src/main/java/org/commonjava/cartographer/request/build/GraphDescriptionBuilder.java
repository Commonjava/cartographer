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
package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.graph.preset.CommonPresetParameters;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.request.GraphDescription;

import java.util.*;

public class GraphDescriptionBuilder
{

    public static GraphDescriptionBuilder newGraphDescriptionBuilder()
    {
        return new GraphDescriptionBuilder();
    }

    Set<ProjectVersionRef> roots;

    private ProjectRelationshipFilter filter;

    private String preset;

    private Map<String, String> presetParams;

    public GraphDescriptionBuilder withRoots( final ProjectVersionRef... refs )
    {
        this.roots = new HashSet<>( Arrays.asList( refs ) );
        return this;
    }

    public GraphDescriptionBuilder withRoots( final Collection<ProjectVersionRef> refs )
    {
        this.roots = new HashSet<>( refs );
        return this;
    }

    public GraphDescriptionBuilder withFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
        return this;
    }

    public GraphDescriptionBuilder withPreset( String preset )
    {
        this.preset = preset;
        return this;
    }

    public synchronized GraphDescriptionBuilder withPresetParam( String key, String value )
    {
        if ( presetParams == null )
        {
            presetParams = new TreeMap<>();
        }

        presetParams.put( key, value );
        return this;
    }

    public synchronized GraphDescriptionBuilder withScopePresetParam( DependencyScope scope )
    {
        if ( presetParams == null )
        {
            presetParams = new TreeMap<>();
        }

        presetParams.put( CommonPresetParameters.SCOPE, scope.realName() );
        return this;
    }

    public synchronized GraphDescriptionBuilder withManagedPresetParam( boolean includeManaged )
    {
        if ( presetParams == null )
        {
            presetParams = new TreeMap<>();
        }

        presetParams.put( CommonPresetParameters.MANAGED, Boolean.toString( includeManaged ) );
        return this;
    }

    public GraphDescriptionBuilder withPresetParams( Map<String, String> params )
    {
        presetParams = new TreeMap<>( params );
        return this;
    }

    public GraphDescription build()
    {
        if ( preset != null )
        {
            return new GraphDescription( preset, null, presetParams, roots );
        }
        else
        {
            return new GraphDescription( filter, null, roots );
        }
    }

}
