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
package org.commonjava.maven.cartographer.testutil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

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

    @Override
    public boolean includeManagedRelationships()
    {
        return true;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        return true;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        return new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) );
    }
}
