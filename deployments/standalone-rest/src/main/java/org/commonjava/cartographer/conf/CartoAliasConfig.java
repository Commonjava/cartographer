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

import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;
import org.commonjava.web.config.section.MapSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.join;

@SectionName( CartoAliasConfig.SECTION_NAME )
@ApplicationScoped
public class CartoAliasConfig
        extends MapSectionListener
{
    public static final String INDY_URL_KEY = "indy.url";

    public static final String ALIAS_PREFIX= "alias.";

    public static final String SECTION_NAME = "aliases";

    private String indyUrl;

    private Map<String, String> explicitAliases = new HashMap<>();

    public String getIndyUrl()
    {
        return indyUrl;
    }

    public void setIndyUrl (String indyUrl )
    {
        this.indyUrl = indyUrl;
    }

    public Map<String, String> getExplicitAliases()
    {
        return explicitAliases;
    }

    public void setExplicitAliases( Map<String, String> explicitAliases )
    {
        this.explicitAliases = explicitAliases;
    }

    @Override
    public void parameter( final String name, final String value )
            throws ConfigurationException
    {
        if ( INDY_URL_KEY.equals( name ) )
        {
            indyUrl = value;
        }
        else if ( name.startsWith( ALIAS_PREFIX ) )
        {
            String key = name.substring( ALIAS_PREFIX.length() );
            explicitAliases.put( key, value );
        }
        else
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn( "IGNORING unknown [{}] configuration: '{}={}'", SECTION_NAME, name, value );
        }
    }

    @Override
    public void sectionStarted( final String name )
            throws ConfigurationException
    {
        // NOP; just block map init in the underlying implementation.
    }

}
