/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.join;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@ApplicationScoped
public class CartoDeploymentConfig
{

    public static final String CARTO_CONFIG_DIR_SYSPROP = "cartographer.config.dir";

    public static final String CARTO_DATA_DIR_SYSPROP = "data.dir";

    public static final String CARTO_WORK_DIR_SYSPROP = "work.dir";

    public static final String DEFAULT_WEBFILTER_PRESET = "default.webfilter.preset";

    public static final String DEFAULT_CARTO_CONFIG = "/etc/cartographer/main.conf";

    private static final String DEFAULT_DEF_WEBFILTER_PRESET = "build-requires";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private File dataBasedir;

    private String defaultWebFilterPreset = DEFAULT_DEF_WEBFILTER_PRESET;

    private File workBasedir;

    private File configDir;

    private boolean configured;

    public File getDataBasedir()
    {
        return dataBasedir;
    }

    @ConfigName( CartoDeploymentConfig.CARTO_DATA_DIR_SYSPROP )
    public void setDataBasedir( File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
    }

    public File getWorkBasedir()
    {
        return workBasedir;
    }

    @ConfigName( CartoDeploymentConfig.CARTO_WORK_DIR_SYSPROP )
    public void setWorkBasedir( File workBasedir )
    {
        this.workBasedir = workBasedir;
    }

    public String getDefaultWebFilterPreset()
    {
        return defaultWebFilterPreset;
    }

    @ConfigName( CartoDeploymentConfig.DEFAULT_WEBFILTER_PRESET )
    public void setDefaultWebFilterPreset( final String preset )
    {
        this.defaultWebFilterPreset = preset;
    }

    public File getConfigDir()
    {
        return configDir;
    }

    public void setConfigDir( File configDir )
    {
        this.configDir = configDir;
    }

    public void configurationDone()
    {
        configured = true;
    }

    public String getValidationErrors()
    {
        List<String> errors = new ArrayList<>();
        
        if ( isEmpty( getDataBasedir() ) )
        {
            errors.add( String.format( "Cartographer File '%s' is required.", CARTO_DATA_DIR_SYSPROP ) );
        }

        if ( isEmpty( getWorkBasedir() ) )
        {
            errors.add( String.format( "Cartographer File '%s' is required.", CARTO_WORK_DIR_SYSPROP ) );
        }

        if ( isEmpty( getConfigDir() ) )
        {
            errors.add( String.format( "Cartographer File '%s' is required.", CARTO_CONFIG_DIR_SYSPROP ) );
        }

        if ( !errors.isEmpty() )
        {
            return join( errors, "\n" );
        }

        return null;
    }

    private void checkConfigured()
    {
        if ( !configured )
        {
            throw new IllegalStateException( "The cartographer system has not been configured! "
                                                     + "This is a sign that something is in the wrong order in the boot sequence!!" );
        }
    }

    private boolean isEmpty( File file )
    {
        if ( null == file )
        {
            return true;
        }
        else if ( !file.exists() && !file.mkdirs() )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
