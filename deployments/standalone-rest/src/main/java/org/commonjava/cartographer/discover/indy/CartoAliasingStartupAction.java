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
package org.commonjava.cartographer.discover.indy;

import org.commonjava.cartographer.INTERNAL.graph.discover.SourceManagerImpl;
import org.commonjava.cartographer.conf.CartoAliasConfig;
import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.model.core.dto.EndpointView;
import org.commonjava.indy.model.core.dto.EndpointViewListing;
import org.commonjava.propulsor.lifecycle.AppLifecycleException;
import org.commonjava.propulsor.lifecycle.StartupAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.ApplicationLaunchException;

import javax.inject.Inject;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;

public class CartoAliasingStartupAction
        implements StartupAction
{

    @Inject
    private CartoAliasConfig config;

    @Inject
    private SourceManagerImpl sourceManager;

    @Override
    public void start()
            throws AppLifecycleException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        String baseUrl = config.getIndyUrl();
        if ( !isEmpty( baseUrl ) )
        {
            try
            {
                Indy indy = new Indy( baseUrl ).connect();
                EndpointViewListing endpoints = indy.stats().getAllEndpoints();
                for ( EndpointView epv : endpoints.getItems() )
                {
                    logger.info( "Alias Indy '{}' => {}", epv.getKey(), epv.getResourceUri() );
                    sourceManager.withAlias( epv.getKey(), epv.getResourceUri() );
                }
            }
            catch ( IndyClientException e )
            {
                throw new AppLifecycleException( "Failed to read repositories from Indy at: %s. Reason: %s", e, baseUrl,
                                                 e.getMessage() );
            }
        }
        else
        {
            logger.info( "No Indy server configured. Skipping auto-aliasing step." );
        }

        Map<String, String> explicitAliases = config.getExplicitAliases();
        if ( explicitAliases != null )
        {
            explicitAliases.forEach( ( alias, url ) -> {
                logger.info( "Alias '{}' => {}", alias, url );
                sourceManager.withAlias( alias, url );
            } );
        }
    }

    @Override
    public String getId()
    {
        return null;
    }

    @Override
    public int getPriority()
    {
        return 0;
    }
}