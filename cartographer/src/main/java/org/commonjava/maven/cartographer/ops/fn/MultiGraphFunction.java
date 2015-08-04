package org.commonjava.maven.cartographer.ops.fn;

import java.util.Map;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.GraphDescription;

public interface MultiGraphFunction<T>
{
    void extract( T elements, Map<GraphDescription, RelationshipGraph> graphs )
        throws CartoDataException;
}
