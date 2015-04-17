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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.atservice.annotation.Service;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.DependencyScope;

@Named( "scope-with-embedded" )
@ApplicationScoped
@Service( PresetFactory.class )
public class ScopeWithEmbeddedProjectsFilterFactory
    implements PresetFactory
{
    public static final String[] IDS = { "sob-build", "scope-with-embedded", "requires", "managed-sob-build",
        "managed-scope-with-embedded", "managed-requires" };

    @Override
    public ProjectRelationshipFilter newFilter( final String presetId, final Map<String, Object> parameters )
    {
        DependencyScope scope = (DependencyScope) parameters.get( CommonPresetParameters.SCOPE );
        if ( scope == null )
        {
            scope = DependencyScope.runtime;
        }

        Boolean managed = (Boolean) parameters.get( CommonPresetParameters.MANAGED );
        if ( managed == null )
        {
            managed = presetId.startsWith( "managed" );
        }

        return new ScopeWithEmbeddedProjectsFilter( scope, managed );
    }

    @Override
    public String[] getPresetIds()
    {
        return IDS;
    }
}
