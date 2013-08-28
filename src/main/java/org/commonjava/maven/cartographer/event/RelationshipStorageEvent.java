package org.commonjava.maven.cartographer.event;

import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class RelationshipStorageEvent
{

    private final Set<ProjectRelationship<?>> stored;

    private final Set<ProjectRelationship<?>> rejected;

    public RelationshipStorageEvent( final Set<ProjectRelationship<?>> stored, final Set<ProjectRelationship<?>> rejected )
    {
        this.stored = stored;
        this.rejected = rejected;
    }

    public final Set<ProjectRelationship<?>> getStored()
    {
        return stored;
    }

    public final Set<ProjectRelationship<?>> getRejected()
    {
        return rejected;
    }

}
