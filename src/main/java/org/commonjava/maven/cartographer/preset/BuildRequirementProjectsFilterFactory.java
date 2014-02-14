/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.cartographer.preset;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.atservice.annotation.Service;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;

@Named( "build-env" )
@ApplicationScoped
@Service( PresetFactory.class )
public class BuildRequirementProjectsFilterFactory
    implements PresetFactory
{
    public static final String[] IDS = { "sob", "build-env", "build-requires", "br", "managed-sob", "managed-build-env", "managed-build-requires",
        "managed-br" };

    @Override
    public ProjectRelationshipFilter newFilter( final String presetId, final GraphWorkspace workspace, final Map<String, Object> parameters )
    {
        Boolean managed = (Boolean) parameters.get( CommonPresetParameters.MANAGED );
        if ( managed == null )
        {
            managed = presetId.startsWith( "managed" );
        }

        return new BuildRequirementProjectsFilter( managed );
    }

    @Override
    public String[] getPresetIds()
    {
        return IDS;
    }
}
