package org.commonjava.maven.cartographer.ops.fn;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface ProjectProjector<T>
{
    T extract( ProjectVersionRef ref, RelationshipGraph graph );
}