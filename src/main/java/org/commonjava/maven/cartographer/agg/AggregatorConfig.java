package org.commonjava.maven.cartographer.agg;

import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class AggregatorConfig
{

    private final Set<ProjectVersionRef> roots;

    public AggregatorConfig( final Set<ProjectVersionRef> roots )
    {
        this.roots = roots;
    }

    public ProjectVersionRef[] getRoots()
    {
        return roots.toArray( new ProjectVersionRef[] {} );
    }

}
