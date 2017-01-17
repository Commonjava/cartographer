/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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