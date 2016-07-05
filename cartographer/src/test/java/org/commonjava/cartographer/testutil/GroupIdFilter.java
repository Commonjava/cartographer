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
package org.commonjava.cartographer.testutil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;

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
    public boolean accept( final ProjectRelationship<?, ?> rel )
    {
        return groupId.equals( rel.getTarget()
                                  .getGroupId() );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        //        return new GroupIdFilter( groupId + ".child" );
        return this;
    }

    @Override
    public Set<ProjectRef> getDepExcludes()
    {
        return null;
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
