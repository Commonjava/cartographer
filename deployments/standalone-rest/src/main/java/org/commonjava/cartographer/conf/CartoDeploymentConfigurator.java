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
package org.commonjava.cartographer.conf;

import org.apache.commons.lang.StringUtils;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.config.Configurator;
import org.commonjava.propulsor.config.ConfiguratorException;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.ConfigurationListener;
import org.commonjava.web.config.dotconf.DotConfConfigurationReader;
import org.commonjava.web.config.io.ConfigFileUtils;
import org.commonjava.web.config.section.BeanSectionListener;
import org.commonjava.web.config.section.ConfigurationSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@ApplicationScoped
public class CartoDeploymentConfigurator
        implements Configurator
{
    @Inject
    private CartographerConfig cartoConfig;

    @Inject
    private Instance<ConfigurationSectionListener> configListeners;

    @Inject
    private Instance<CartoSubConfig> subConfigs;

    @Override
    public void load( BootOptions options )
            throws ConfiguratorException
    {
        String config = options.getConfig();
        File configFile = new File( config );

        cartoConfig.setConfigDir( configFile.getAbsoluteFile().getParentFile() );
        cartoConfig.setHomeDir( new File( options.getHomeDir() ) );

        if ( !configFile.exists() )
        {
            // TODO: Make resilient enough to write default configs.
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn( "Cannot find configuration file: {}. Using application defaults.", configFile );

//            throw new ConfiguratorException( "Missing configuration: %s", configFile );
        }
        else
        {
            try (InputStream stream = ConfigFileUtils.readFileWithIncludes( config, System.getProperties() ) )
            {
                List<ConfigurationSectionListener<?>> listeners = new ArrayList<>();
                listeners.add( new BeanSectionListener( cartoConfig  ) );

                if ( configListeners != null )
                {
                    configListeners.forEach( (listener)->listeners.add( listener ) );
                }

                if ( subConfigs != null )
                {
                    subConfigs.forEach( subConfig -> listeners.add( new BeanSectionListener<>( subConfig ) ) );
                }

                new DotConfConfigurationReader( listeners ).loadConfiguration( stream );
            }
            catch ( ConfigurationException | IOException e )
            {
                throw new ConfiguratorException( "Failed to read configuration: %s. Reason: %s", e, configFile,
                                                 e.getMessage() );
            }
        }

        cartoConfig.configurationDone();
        String validationErrors = cartoConfig.getValidationErrors();
        if ( isNotEmpty( validationErrors ) )
        {
            throw new ConfiguratorException( "Cartographer configuration is not complete!\n\n%s", validationErrors );
        }
    }
}
