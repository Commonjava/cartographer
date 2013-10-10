package org.commonjava.maven.cartographer.agg;

import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.cartographer.data.CartoDataException;

public interface GraphAggregator
{

    EProjectGraph connectIncomplete( EProjectGraph graph, AggregationOptions crawlerConfig )
        throws CartoDataException;

    EProjectNet connectIncomplete( EProjectWeb graph, AggregationOptions crawlerConfig )
        throws CartoDataException;

}