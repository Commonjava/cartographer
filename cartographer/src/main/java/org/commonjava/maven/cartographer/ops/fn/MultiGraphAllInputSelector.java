package org.commonjava.maven.cartographer.ops.fn;

import java.util.Set;
import java.util.function.Supplier;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MultiGraphAllInputSelector
    implements FunctionInputSelector<MultiGraphAllInput>
{

    @Override
    public MultiGraphAllInput select( final Supplier<Set<ProjectVersionRef>> allProjects,
                                      final Supplier<Set<ProjectRelationship<?>>> allRelationships,
                                      final Supplier<Set<ProjectVersionRef>> roots )
    {
        return new MultiGraphAllInput( allProjects, allRelationships, roots );
    }

}
