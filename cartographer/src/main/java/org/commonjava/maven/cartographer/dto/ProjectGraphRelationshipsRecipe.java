package org.commonjava.maven.cartographer.dto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.filter.RelationshipTypeFilter;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class ProjectGraphRelationshipsRecipe
    extends ProjectGraphRecipe
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
