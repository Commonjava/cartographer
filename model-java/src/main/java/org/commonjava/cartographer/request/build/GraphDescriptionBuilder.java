package org.commonjava.cartographer.request.build;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.request.GraphDescription;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GraphDescriptionBuilder
{

    public static GraphDescriptionBuilder newGraphDescriptionBuilder()
    {
        return new GraphDescriptionBuilder();
    }

    Set<ProjectVersionRef> roots;

    private ProjectRelationshipFilter filter;

    private String preset;

    public GraphDescriptionBuilder withRoots( final ProjectVersionRef... refs )
    {
        this.roots = new HashSet<>( Arrays.asList( refs ) );
        return this;
    }

    public GraphDescriptionBuilder withRoots( final Collection<ProjectVersionRef> refs )
    {
        this.roots = new HashSet<>( refs );
        return this;
    }

    public GraphDescriptionBuilder withFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
        return this;
    }

    public GraphDescriptionBuilder withPreset( String preset )
    {
        this.preset = preset;
        return this;
    }

    public GraphDescription build()
    {
        GraphDescription desc = new GraphDescription( filter, roots );
        desc.setPreset( preset );

        return desc;
    }

}
