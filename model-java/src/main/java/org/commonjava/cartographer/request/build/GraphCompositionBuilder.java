package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphCalculationType;
import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.GraphDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraphCompositionBuilder
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static GraphCompositionBuilder newGraphCompositionBuilder()
    {
        return new GraphCompositionBuilder();
    }

    private GraphCalculationType calculation;

    private final List<GraphDescription> graphs = new ArrayList<>();

    public GraphCompositionBuilder withCalculation( final GraphCalculationType calculation )
    {
        this.calculation = calculation;
        return this;
    }

    public GraphCompositionBuilder withGraph( final GraphDescription description )
    {
        if ( !graphs.isEmpty() && calculation == null )
        {
            calculation = GraphCalculationType.ADD;
        }

        graphs.add( description );
        return this;
    }

    public GraphComposition build()
    {
        return new GraphComposition( calculation, graphs );
    }

}
