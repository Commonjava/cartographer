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

import org.apache.commons.lang.StringUtils;
import org.commonjava.cartographer.CartoException;
import org.commonjava.cartographer.INTERNAL.graph.discover.SourceManagerImpl;
import org.commonjava.cartographer.conf.CartoAliasConfig;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.model.core.dto.EndpointView;
import org.commonjava.indy.model.core.dto.EndpointViewListing;
import org.commonjava.propulsor.lifecycle.AppLifecycleException;
import org.commonjava.propulsor.lifecycle.StartupAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.ApplicationLaunchException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;

@ApplicationScoped
public class CartoAliasingStartupAction
        implements StartupAction
{

    @Inject
    private CartoAliasConfig config;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Override
    public void start()
            throws AppLifecycleException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "STARTUP/begin: Alias initialization" );

        String baseUrl = config.getIndyUrl();
        if ( !isEmpty( baseUrl ) )
        {
            logger.info( "Retrieving endpoints from Indy at: {} in order to auto-alias...", baseUrl );
            try
            {
                Indy indy = new Indy( baseUrl ).connect();
                EndpointViewListing endpoints = indy.stats().getAllEndpoints();
                for ( EndpointView epv : endpoints.getItems() )
                {
                    logger.info( "Alias Indy '{}' => {}", epv.getKey(), epv.getResourceUri() );
                    sourceManager.addSourceAlias( epv.getKey(), epv.getResourceUri() );
                }
            }
            catch ( IndyClientException e )
            {
                throw new AppLifecycleException( "Failed to read repositories from Indy at: %s. Reason: %s", e, baseUrl,
                                                 e.getMessage() );
            }
            catch ( CartoException e )
            {
                throw new AppLifecycleException( "Failed to add alias from Indy at: %s. Reason: %s", e, baseUrl,
                                                 e.getMessage() );
            }
        }
        else
        {
            logger.info( "No Indy server configured. Skipping auto-aliasing step." );
        }

        Map<String, String> explicitAliases = config.getExplicitAliases();
        List<String> errors = new ArrayList<>();
        if ( explicitAliases != null )
        {
            logger.info( "Adding explicit aliases from configuration..." );
            explicitAliases.forEach( ( alias, url ) -> {
                logger.info( "Alias '{}' => {}", alias, url );
                try
                {
                    sourceManager.addSourceAlias( alias, url );
                }
                catch ( CartoException e )
                {
                    errors.add( String.format( "%s -> %s (Reason: %s)", e, alias, url,
                                               e.getMessage() ) );
                }
            } );
        }

        if ( !errors.isEmpty() )
        {
            throw new AppLifecycleException( "Failed to add aliases:\n  %s", StringUtils.join( errors, "\n  " ) );
        }

        logger.info( "STARTUP/done: Alias initialization complete." );
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