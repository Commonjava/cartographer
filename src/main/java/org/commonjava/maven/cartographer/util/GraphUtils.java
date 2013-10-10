package org.commonjava.maven.cartographer.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class GraphUtils
{

    private GraphUtils()
    {
    }

    public static Set<ProjectVersionRef> targets( final ProjectRelationship<?>... relationships )
    {
        return targets( Arrays.asList( relationships ) );
    }

    public static Set<ProjectVersionRef> targets( final Collection<ProjectRelationship<?>> relationships )
    {
        final Set<ProjectVersionRef> results = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : relationships )
        {
            results.add( rel.getTarget() );
        }

        return results;
    }

    public static Set<ProjectVersionRef> gavs( final ProjectRelationship<?>... relationships )
    {
        return gavs( Arrays.asList( relationships ) );
    }

    public static Set<ProjectVersionRef> gavs( final Collection<ProjectRelationship<?>> relationships )
    {
        final Set<ProjectVersionRef> results = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : relationships )
        {
            results.add( rel.getDeclaring()
                            .asProjectVersionRef() );

            results.add( rel.getTarget()
                            .asProjectVersionRef() );
        }

        return results;
    }
}
