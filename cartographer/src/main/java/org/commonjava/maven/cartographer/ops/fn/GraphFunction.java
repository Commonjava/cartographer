package org.commonjava.maven.cartographer.ops.fn;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;

@FunctionalInterface
public interface GraphFunction
{
    void extract( RelationshipGraph graph )
                    throws CartoDataException, CartoRequestException;
}