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
package org.commonjava.cartographer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.cartographer.ops.CalculationOps;
import org.commonjava.cartographer.ops.GraphOps;
import org.commonjava.cartographer.ops.GraphRenderingOps;
import org.commonjava.cartographer.ops.MetadataOps;
import org.commonjava.cartographer.ops.ResolveOps;
import org.commonjava.cartographer.request.AbstractGraphRequest;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.propulsor.client.http.ClientHttpException;
import org.commonjava.propulsor.client.http.ClientHttpSupport;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.ClientAuthenticator;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientCartographer
        implements Cartographer
{

    private boolean internalClient;

    private CartographerRESTClient client;

    private Map<String, String> sourceAliases;

    public ClientCartographer( ClientHttpSupport http )
    {
        this.client = new CartographerRESTClient( http );
        this.internalClient = true;
    }

    public ClientCartographer( final String baseUrl, final ClientAuthenticator authenticator )
            throws ClientHttpException
    {
        this.client = new CartographerRESTClient( baseUrl, authenticator );
        this.internalClient = true;
    }

    public ClientCartographer( final SiteConfig siteConfig, final HttpFactory httpFactory )
            throws ClientHttpException
    {
        this.client = new CartographerRESTClient( siteConfig, httpFactory );
        this.internalClient = true;
    }

    public ClientCartographer( String baseUrl, PasswordManager passwordManager )
            throws CartoClientException, ClientHttpException
    {
        this.client = new CartographerRESTClient( baseUrl, passwordManager );
        this.internalClient = true;
    }

    public ClientCartographer( CartographerRESTClient client )
    {
        this.client = client;
        this.internalClient = false;
    }

    public synchronized ClientCartographer setSourceAliases( Map<String, String> sourceAliases )
    {
        if ( sourceAliases != null )
        {
            this.sourceAliases = sourceAliases;
        }
        return this;
    }

    public synchronized ClientCartographer setSourceAlias( String alias, String source )
    {
        if ( sourceAliases == null )
        {
            sourceAliases = new HashMap<>();
        }

        sourceAliases.put(alias, source );
        return this;
    }

    <T extends AbstractGraphRequest> T normalizeRequest( T request )
    {
        request.setSource( deAlias( request.getSource() ) );
        return request;
    }

    <T extends GraphAnalysisRequest> T normalizeRequests( T request )
    {
        for ( MultiGraphRequest req: request.getGraphRequests() )
        {
            req.setSource( deAlias( req.getSource() ) );
        }
        return request;
    }

    String deAlias( String source )
    {
        if ( sourceAliases != null )
        {
            String deref = sourceAliases.get( source );
            if ( deref != null )
            {
                return deref;
            }
        }

        return source;
    }

    @Override
    public ObjectMapper getObjectMapper()
    {
        return client.getObjectMapper();
    }

    @Override
    public CalculationOps getCalculator()
    {
        return new ClientCalculatorOps( this, client );
    }

    @Override
    public GraphOps getGrapher()
    {
        return new ClientGraphOps( this, client );
    }

    @Override
    public GraphRenderingOps getRenderer()
    {
        return new ClientGraphRenderingOps( this, client );
    }

    @Override
    public MetadataOps getMetadata()
    {
        return new ClientMetadataOps( this, client );
    }

    @Override
    public ResolveOps getResolver()
    {
        return new ClientResolverOps( this, client );
    }

    public void close()
            throws Exception
    {
        if ( internalClient )
        {
            client.close();
        }
    }
}
