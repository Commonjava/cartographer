package org.commonjava.cartographer.graph.fn;

import java.util.Set;
import java.util.function.Supplier;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MultiGraphAllInput
{

    private final Supplier<Set<ProjectVersionRef>> allRefs;

    private final Supplier<Set<ProjectRelationship<?>>> allRels;

    private final Supplier<Set<ProjectVersionRef>> roots;

    public MultiGraphAllInput( final Supplier<Set<ProjectVersionRef>> allRefs,
                               final Supplier<Set<ProjectRelationship<?>>> allRels,
                               final Supplier<Set<ProjectVersionRef>> roots )
    {
        this.allRefs = allRefs;
        this.allRels = allRels;
        this.roots = roots;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return allRefs.get();
    }

    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return allRels.get();
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots.get();
    }

}
