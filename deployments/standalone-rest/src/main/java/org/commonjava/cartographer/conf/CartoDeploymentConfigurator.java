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
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.config.Configurator;
import org.commonjava.propulsor.config.ConfiguratorException;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.ConfigurationListener;
import org.commonjava.web.config.DefaultConfigurationListener;
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
    private Instance<CartoSubConfig> subConfigs;

    @Override
    public void load( BootOptions options )
            throws ConfiguratorException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        String config = options.getConfig();
        File configFile = new File( config );

        cartoConfig.setHomeDir( new File( options.getHomeDir() ) );

        if ( !configFile.exists() )
        {
            File homeConfigFile = new File( cartoConfig.getConfigDir(), "main.conf" );

            // TODO: Make resilient enough to write default configs.
            logger.warn( "Cannot find configuration file: {}. Trying: {}", configFile, homeConfigFile );

            if ( homeConfigFile.exists() )
            {
                logger.warn( "Using configuration file from home directory: {}", homeConfigFile );
                configFile = homeConfigFile;
            }
            else
            {
                logger.warn( "Cannot find home directory configuration file: {}. Using built-in application defaults.", homeConfigFile );
                return;
            }
//            throw new ConfiguratorException( "Missing configuration: %s", configFile );
        }

        cartoConfig.setConfigDir( configFile.getAbsoluteFile().getParentFile() );

        try (InputStream stream = ConfigFileUtils.readFileWithIncludes( configFile, System.getProperties() ) )
        {
            DefaultConfigurationListener configListener = new DefaultConfigurationListener( new BeanSectionListener( cartoConfig ));

            if ( subConfigs != null )
            {
                for ( CartoSubConfig subConfig : subConfigs )
                {
                    logger.debug( "Adding configuration section listener for: {}", subConfig );
                    configListener.with(subConfig);
                }
            }

            new DotConfConfigurationReader( configListener ).loadConfiguration( stream );
        }
        catch ( ConfigurationException | IOException e )
        {
            throw new ConfiguratorException( "Failed to read configuration: %s. Reason: %s", e, configFile,
                                             e.getMessage() );
        }

        cartoConfig.configurationDone();
        String validationErrors = cartoConfig.getValidationErrors();
        if ( isNotEmpty( validationErrors ) )
        {
            throw new ConfiguratorException( "Cartographer configuration is not complete!\n\n%s", validationErrors );
        }
    }
}
