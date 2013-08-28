package org.commonjava.maven.cartographer.data;

import java.util.Collection;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public interface StorageListener
{

    void relationshipsStored( Collection<ProjectRelationship<?>> stored, Set<ProjectRelationship<?>> rejected )
        throws CartoDataException;

}
