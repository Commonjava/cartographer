package org.commonjava.cartographer.graph.fn;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.CartoDataException;

@FunctionalInterface
public interface GraphFunction
{
    void extract( RelationshipGraph graph )
                    throws CartoDataException, CartoRequestException;
}