package org.commonjava.cartographer.data.model;

import java.util.UUID;

/**
 * Unique key for a directed relationship between some {@link PkgVersion} source and another {@link PkgId} target. The
 * relationship itself may contain version advice for the target package.
 */
public class RelationshipId
{
    private final String relationshipId;

    public static RelationshipId generate()
    {
        return new RelationshipId( UUID.randomUUID().toString() );
    }

    public RelationshipId( final String relationshipId )
    {
        this.relationshipId = relationshipId;
    }

    @Override
    public String toString()
    {
        return "RelationshipId[" + relationshipId + ']';
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof RelationshipId ) )
        {
            return false;
        }

        final RelationshipId that = (RelationshipId) o;

        return relationshipId.equals( that.relationshipId );
    }

    @Override
    public int hashCode()
    {
        return relationshipId.hashCode();
    }
}
