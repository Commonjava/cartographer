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
package org.commonjava.maven.cartographer.testutil;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class GroupIdFilter
    implements ProjectRelationshipFilter
{

    private static final long serialVersionUID = 1L;

    private final String groupId;

    public GroupIdFilter( final String groupId )
    {
        this.groupId = groupId;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        return groupId.equals( rel.getTarget()
                                  .getGroupId() );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        //        return new GroupIdFilter( groupId + ".child" );
        return this;
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public String getLongId()
    {
        return "GROUP_ID[" + groupId + "]";
    }

    @Override
    public String getCondensedId()
    {
        return getLongId();
    }
}
