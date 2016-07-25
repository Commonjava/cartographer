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
package org.commonjava.cartographer.discover;

import org.commonjava.cartographer.INTERNAL.graph.discover.SourceManagerImpl;
import org.commonjava.cartographer.conf.CartoDeploymentConfig;
import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.model.core.dto.EndpointView;
import org.commonjava.indy.model.core.dto.EndpointViewListing;
import org.commonjava.propulsor.lifecycle.AppLifecycleException;
import org.commonjava.propulsor.lifecycle.StartupAction;

import javax.inject.Inject;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;

public class CartoAliasStartupAction
        implements StartupAction
{

    @Inject
    private CartoDeploymentConfig config;

    @Inject
    private SourceManagerImpl sourceManager;

    @Override
    public void start()
            throws AppLifecycleException
    {
        String baseUrl = config.getIndyUrl();
        if ( null == baseUrl || baseUrl.isEmpty() )
        {
            return;
        }

        try
        {
            Indy indy = new Indy( baseUrl ).connect();
            EndpointViewListing endpoints = indy.stats().getAllEndpoints();
            for ( EndpointView epv : endpoints.getItems() )
            {
                sourceManager.withAlias( epv.getKey(), epv.getResourceUri() );
            }
        }
        catch ( IndyClientException e )
        {
            throwError( e );
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