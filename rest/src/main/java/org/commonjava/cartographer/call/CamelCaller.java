package org.commonjava.cartographer.call;

import org.apache.camel.CamelContext;
import org.commonjava.propulsor.deploy.camel.route.EndpointAliasManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This class simplifies the process of calling into a Camel route.
 */
@ApplicationScoped
public class CamelCaller
{
    @Inject
    private CamelContext camel;

    @Inject
    private EndpointAliasManager aliases;

    public <T> CompletableFuture<T> callAsync(String endpointKey, Object body, Map<String, Object> headers, Class<T> resultType)
    {
        return camel.createProducerTemplate()
             .asyncRequestBodyAndHeaders( aliases.lookup( endpointKey ), body, headers, resultType );
    }
}
