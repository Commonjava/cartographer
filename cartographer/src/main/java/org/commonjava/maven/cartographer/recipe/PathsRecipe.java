package org.commonjava.maven.cartographer.recipe;

import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;

public class PathsRecipe
    extends MultiGraphResolverRecipe
{

    private Set<ProjectRef> targets;

    public Set<ProjectRef> getTargets()
    {
        return targets;
    }

    public void setTargets( final Set<ProjectRef> targets )
    {
        this.targets = targets;
    }

}
