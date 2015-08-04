package org.commonjava.maven.cartographer.ops.fn;

import java.util.Set;
import java.util.function.Supplier;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface FunctionInputSelector<T>
{

    T select( Supplier<Set<ProjectVersionRef>> allProjects,
                   Supplier<Set<ProjectRelationship<?>>> allRelationships,
               Supplier<Set<ProjectVersionRef>> roots );

}
