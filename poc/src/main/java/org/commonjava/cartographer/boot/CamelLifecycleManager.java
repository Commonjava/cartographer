package org.commonjava.cartographer.boot;

import org.apache.camel.main.Main;
import org.commonjava.cartographer.structure.CartoCamelContextualizer;
import org.commonjava.cartographer.structure.CartoRouteBuilder;
import org.commonjava.propulsor.lifecycle.AppLifecycleException;
import org.commonjava.propulsor.lifecycle.ShutdownAction;
import org.commonjava.propulsor.lifecycle.StartupAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by jdcasey on 7/10/17.
 */
@ApplicationScoped
public class CamelLifecycleManager
        implements StartupAction, ShutdownAction
{

    private Main camelMain;

    @Inject
    private Instance<CartoRouteBuilder> routeBuilders;

    @Inject
    private Instance<CartoCamelContextualizer> contextualizers;

    @Produces
    public Main getCamelMain()
    {
        return camelMain;
    }

    @Override
    public void shutdown()
    {
        try
        {
            camelMain.shutdown();
        }
        catch ( Exception e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Camel shutdown failed.", e );
        }
    }

    @Override
    public void start()
            throws AppLifecycleException
    {
        camelMain = new Main();

        if ( contextualizers != null )
        {
            for ( CartoCamelContextualizer ccc : contextualizers )
            {
                ccc.contextualize( camelMain.getOrCreateCamelContext() );
            }
        }

        if ( routeBuilders != null )
        {
            for ( CartoRouteBuilder rb : routeBuilders )
            {
                try
                {
                    rb.addRoutesToCamelContext( camelMain.getOrCreateCamelContext() );
                }
                catch ( Exception e )
                {
                    throw new AppLifecycleException( "Could not configure Camel via: %s. Reason: %s", e, rb, e.getMessage() );
                }
            }
        }

        try
        {
            camelMain.start();
        }
        catch ( Exception e )
        {
            throw new AppLifecycleException( "Could not start Camel: %s", e, e.getMessage() );
        }
    }

    @Override
    public String getId()
    {
        return "Camel application";
    }

    @Override
    public int getPriority()
    {
        return 1;
    }
}
