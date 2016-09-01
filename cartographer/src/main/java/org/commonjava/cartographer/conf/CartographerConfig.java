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
import javax.enterprise.inject.Alternative;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.join;

@ApplicationScoped
@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
public class CartographerConfig
{

    public static final String CARTO_CONFIG_DIR_SYSPROP = "cartographer.config.dir";

    public static final String CARTO_DATA_DIR_CONFIG = "data.dir";

    public static final String CARTO_WORK_DIR_CONFIG = "work.dir";

    public static final String CARTO_CACHE_DIR_CONFIG = "cache.dir";

    public static final String DEFAULT_WEBFILTER_PRESET_CONFIG = "default.webfilter.preset";

    private static final String DEFAULT_WEBFILTER_PRESET_VALUE = "build-requires";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private String defaultWebFilterPreset;

    private File dataBasedir;

    private File workBasedir;

    private File cacheBasedir;

    private File configDir;

    private boolean configured;

    private File homeDir;

    public File getDataBasedir()
    {
        return getDir( dataBasedir, "data");
    }

    @ConfigName( CartographerConfig.CARTO_DATA_DIR_CONFIG )
    public void setDataBasedir( File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
    }

    public File getWorkBasedir()
    {
        return getDir( workBasedir, "work" );
    }

    @ConfigName( CartographerConfig.CARTO_WORK_DIR_CONFIG )
    public void setWorkBasedir( File workBasedir )
    {
        this.workBasedir = workBasedir;
    }

    public File getCacheBasedir()
    {
        return getDir( cacheBasedir, "cache" );
    }

    @ConfigName( CartographerConfig.CARTO_CACHE_DIR_CONFIG )
    public void setCacheBasedir( File cacheBasedir )
    {
        this.cacheBasedir = cacheBasedir;
    }

    public String getDefaultWebFilterPreset()
    {
        return defaultWebFilterPreset == null ? DEFAULT_WEBFILTER_PRESET_VALUE : defaultWebFilterPreset;
    }

    @ConfigName( CartographerConfig.DEFAULT_WEBFILTER_PRESET_CONFIG )
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
            errors.add( String.format( "Cartographer configuration '%s' is required.", CARTO_DATA_DIR_CONFIG ) );
        }

        if ( isEmpty( getWorkBasedir() ) )
        {
            errors.add( String.format( "Cartographer configuration '%s' is required.", CARTO_WORK_DIR_CONFIG ) );
        }

        if ( isEmpty( getCacheBasedir() ) )
        {
            errors.add( String.format( "Cartographer configuration '%s' is required.", CARTO_WORK_DIR_CONFIG ) );
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

    private File getDir( File dir, String defaultName )
    {
        if ( dir != null )
        {
            return dir;
        }

        return new File( getHomeDir(), defaultName );
    }

    public void setHomeDir( File homeDir )
    {
        this.homeDir = homeDir;
    }

    public File getHomeDir()
    {
        return homeDir;
    }
}
