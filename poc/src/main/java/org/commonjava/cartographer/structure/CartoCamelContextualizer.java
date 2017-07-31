package org.commonjava.cartographer.structure;

import org.apache.camel.CamelContext;
import org.commonjava.propulsor.lifecycle.AppLifecycleException;

/**
 * Created by jdcasey on 7/10/17.
 */
public interface CartoCamelContextualizer
{
    void contextualize( CamelContext orCreateCamelContext )
            throws AppLifecycleException;
}
