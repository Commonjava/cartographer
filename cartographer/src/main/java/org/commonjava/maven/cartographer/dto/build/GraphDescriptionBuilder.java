package org.commonjava.maven.cartographer.dto.build;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.dto.GraphDescription;

public class GraphDescriptionBuilder<T extends GraphDescriptionOwner<T>>
{

    public static final class StandaloneDescriptionOwner
        implements GraphDescriptionOwner<StandaloneDescriptionOwner>
    {
        private GraphDescription graph;

        @Override
        public StandaloneDescriptionOwner withGraph( final GraphDescription graph )
        {
            this.graph = graph;
            return this;
        }

        public GraphDescription getGraph()
        {
            return graph;
        }
    }

    public static GraphDescriptionBuilder<StandaloneDescriptionOwner> newGraphDescriptionBuilder()
    {
        return new GraphDescriptionBuilder<>( new StandaloneDescriptionOwner() );
    }

    Set<ProjectVersionRef> roots;

    private ProjectRelationshipFilter filter;

    private final T compBuilder;

    public GraphDescriptionBuilder( final T compBuilder )
    {
        this.compBuilder = compBuilder;
    }

    public GraphDescriptionBuilder<T> withRoots( final ProjectVersionRef... refs )
    {
        this.roots = new HashSet<>( Arrays.asList( refs ) );
        return this;
    }

    public GraphDescriptionBuilder<T> withRoots( final Collection<ProjectVersionRef> refs )
    {
        this.roots = new HashSet<>( refs );
        return this;
    }

    public GraphDescriptionBuilder<T> withFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
        return this;
    }

    public T finishGraph()
    {
        if ( compBuilder != null )
        {
            return compBuilder.withGraph( build() );
        }

        return null;
    }

    public GraphDescription build()
    {
        return new GraphDescription( filter, roots );
    }

}
