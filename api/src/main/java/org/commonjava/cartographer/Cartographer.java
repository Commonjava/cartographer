package org.commonjava.cartographer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cartographer.ops.*;

/**
 * Created by jdcasey on 8/14/15.
 */
public interface Cartographer
{
    ObjectMapper getObjectMapper();

    CalculationOps getCalculator();

    GraphOps getGrapher();

    GraphRenderingOps getRenderer();

    MetadataOps getMetadata();

    ResolveOps getResolver();

    void close()
            throws CartoDataException;
}
