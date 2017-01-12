package org.commonjava.cartographer.actions;

import io.swagger.jaxrs.config.BeanConfig;
import org.commonjava.cartographer.conf.CartoSwaggerConfig;
import org.commonjava.propulsor.lifecycle.BootupAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by ruhan on 12/29/16.
 */
@ApplicationScoped
public class SwaggerBootupAction implements BootupAction
{
    @Inject
    CartoSwaggerConfig config;

    @Override
    public void init()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        if ( ! config.isEnabled() )
        {
            logger.warn("Swagger scanner not enabled");
            return;
        }

        logger.info("Swagger scanner enabled");

        final BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage( config.getResourcePackage() );
        beanConfig.setBasePath( config.getBasePath() );
        beanConfig.setLicense( config.getLicense() );
        beanConfig.setLicenseUrl( config.getLicenseUrl() );
        beanConfig.setScan( true );
        beanConfig.setVersion( config.getVersion() );
    }

    @Override
    public String getId() {
        return "SwaggerBootupAction";
    }

    @Override
    public int getPriority() {
        return 0;
    }
}