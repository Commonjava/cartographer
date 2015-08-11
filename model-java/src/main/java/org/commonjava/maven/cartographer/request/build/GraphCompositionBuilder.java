package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.GraphCalculationType;
import org.commonjava.maven.cartographer.request.GraphComposition;
import org.commonjava.maven.cartographer.request.GraphDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraphCompositionBuilder<O extends GraphCompositionOwner<O>>
    implements GraphDescriptionOwner<GraphCompositionBuilder<O>>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final class StandaloneGraphCompositionOwner
        implements GraphCompositionOwner<StandaloneGraphCompositionOwner>
    {
        private GraphComposition graphs;

        @Override
        public StandaloneGraphCompositionOwner withGraphs( final GraphComposition graphs )
        {
            this.graphs = graphs;
            return this;
        }

        public GraphComposition getGraphs()
        {
            return graphs;
        }
    }

    public static GraphCompositionBuilder<StandaloneGraphCompositionOwner> newGraphCompositionBuilder()
    {
        return new GraphCompositionBuilder<>( new StandaloneGraphCompositionOwner() );
    }

    private GraphCalculationType calculation;

    private final List<GraphDescription> graphs = new ArrayList<>();

    private final O owner;

    public GraphCompositionBuilder( final O owner )
    {
        this.owner = owner;
    }

    public GraphDescriptionBuilder<GraphCompositionBuilder<O>> withNewGraph()
    {
        return new GraphDescriptionBuilder<>( this );
    }

    public GraphCompositionBuilder<O> withCalculation( final GraphCalculationType calculation )
    {
        this.calculation = calculation;
        return this;
    }

    @Override
    public GraphCompositionBuilder<O> withGraph( final GraphDescription description )
    {
        if ( !graphs.isEmpty() && calculation == null )
        {
            calculation = GraphCalculationType.ADD;
        }

        graphs.add( description );
        return this;
    }

    public O finishGraphComposition()
    {
        logger.debug( "Sending graphs back to owner: {}", owner );

        if ( owner != null )
        {
            return owner.withGraphs( build() );
        }

        return null;
    }

    public GraphComposition build()
    {
        return new GraphComposition( calculation, graphs );
    }

}
