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

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@ApplicationScoped
public class CartoDeploymentConfig
{

    public static final String CARTO_CONFIG_DIR_SYSPROP = "cartographer.config.dir";

    public static final String DEFAULT_CARTO_CONFIG = "/etc/cartographer/main.conf";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String DEFAULT_DEF_WEBFILTER_PRESET = "build-requires";

    private File dataBasedir;

    private String defaultWebFilterPreset = DEFAULT_DEF_WEBFILTER_PRESET;
    
    private File workBasedir;

    private File configDir;

    private boolean configured;

    public File getDataBasedir()
    {
        return dataBasedir;
    }

    @ConfigName( "data.dir" )
    public void setDataBasedir( File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
    }

    public File getWorkBasedir()
    {
        return workBasedir;
    }

    @ConfigName( "work.dir" )
    public void setWorkBasedir( File workBasedir )
    {
        this.workBasedir = workBasedir;
    }

    public String getDefaultWebFilterPreset()
    {
        return defaultWebFilterPreset;
    }

    @ConfigName( "default.webfilter.preset" )
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

    private void checkConfigured()
    {
        if ( !configured )
        {
            throw new IllegalStateException( "The cartographer system has not been configured! "
                                                     + "This is a sign that something is in the wrong order in the boot sequence!!" );
        }
    }

    public String getValidationErrors()
    {
        return null;
    }
}
