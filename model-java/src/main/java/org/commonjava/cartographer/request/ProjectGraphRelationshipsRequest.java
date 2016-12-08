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
package org.commonjava.cartographer.request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.commonjava.cartographer.graph.filter.AnyFilter;
import org.commonjava.cartographer.graph.filter.ProjectRelationshipFilter;
import org.commonjava.cartographer.graph.filter.RelationshipTypeFilter;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class ProjectGraphRelationshipsRequest
    extends ProjectGraphRequest
{

    private List<RelationshipType> types;

    private boolean managedIncluded;

    private boolean concreteIncluded;

    public List<RelationshipType> getTypes()
    {
        return types;
    }

    public void setTypes( final List<RelationshipType> types )
    {
        this.types = types;
    }

    public boolean isManagedIncluded()
    {
        return managedIncluded;
    }

    public boolean isConcreteIncluded()
    {
        return concreteIncluded;
    }

    public void setManagedIncluded( final boolean managedIncluded )
    {
        this.managedIncluded = managedIncluded;
    }

    public void setConcreteIncluded( final boolean concreteIncluded )
    {
        this.concreteIncluded = concreteIncluded;
    }

    public RelationshipType[] toTypeArray()
    {
        return types == null || types.isEmpty() ? RelationshipType.values()
                        : types.toArray( new RelationshipType[types.size()] );
    }

    public ProjectRelationshipFilter getTypeFilter()
    {
        if ( types == null || types.isEmpty()
            || new HashSet<RelationshipType>( types ).containsAll( Arrays.asList( RelationshipType.values() ) ) )
        {
            return AnyFilter.INSTANCE;
        }
        else
        {
            return new RelationshipTypeFilter( types, isManagedIncluded(), isConcreteIncluded() );
        }
    }

}
