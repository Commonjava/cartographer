/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
    public static final String[] IDS = { "sob-build", "scope-with-embedded", "requires", "managed-sob-build", "managed-scope-with-embedded",
        "managed-requires" };

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
