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
package org.commonjava.cartographer.testutil;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartographerCore;
import org.commonjava.cartographer.CartographerCoreBuilder;
import org.commonjava.cartographer.INTERNAL.graph.agg.DefaultGraphAggregator;
import org.commonjava.cartographer.graph.GraphResolver;
import org.commonjava.cartographer.spi.graph.agg.GraphAggregator;
import org.commonjava.cartographer.spi.graph.discover.ProjectRelationshipDiscoverer;
import org.commonjava.cartographer.ops.ResolveOps;
import org.commonjava.cartographer.graph.RelationshipGraph;
import org.commonjava.cartographer.graph.ViewParams;
import org.commonjava.cartographer.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.cartographer.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.rel.MavenModelProcessor;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.testing.core.transport.TestTransport;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.concurrent.Executors;

public class CartoFixture
    extends ExternalResource
{

    private CartographerCoreBuilder builder;

    private CartographerCore cartographer;

    private boolean graphAggregatorSet;

    private final TestTransport testTransport;

    private final TemporaryFolder temp;

    public CartoFixture()
    {
        this.temp = new TemporaryFolder();
        try
        {
            temp.create();
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
            Assert.fail( "Failed to intialize temp folder manager for CartoFixture: " + e.getMessage() );
        }

        testTransport = new TestTransport();
    }

    public CartoFixture( final TemporaryFolder temp )
    {
        this.temp = temp;
        testTransport = new TestTransport();
    }

    public TemporaryFolder getTemp()
    {
        return temp;
    }

    protected void initCartographer()
        throws Exception
    {
        if ( builder == null )
        {
            final RelationshipGraphConnectionFactory connectionFactory =
                new FileNeo4jConnectionFactory( getTemp().newFolder( "graph.", ".db" ), false );

            builder =
                new CartographerCoreBuilder( temp.newFolder( "cache" ), connectionFactory ).withTransports( testTransport )
                                                                                     .withDiscoverer( new TestAggregatorDiscoverer() );

            if ( !graphAggregatorSet )
            {
                builder.withGraphAggregator( newGraphAggregator( builder.getDiscoverer() ) );
            }
        }
    }

    private GraphAggregator newGraphAggregator( final ProjectRelationshipDiscoverer discoverer )
    {
        return new DefaultGraphAggregator( discoverer, Executors.newFixedThreadPool( 1 ) );
    }

    public TestAggregatorDiscoverer getDiscoverer()
    {
        return (TestAggregatorDiscoverer) builder.getDiscoverer();
    }

    public GraphAggregator getAggregator()
    {
        return builder.getGraphAggregator();
    }

    public ResolveOps getResolveOps()
    {
        return cartographer == null ? null : cartographer.getResolver();
    }

    public void setDiscoverer( final TestAggregatorDiscoverer discoverer )
        throws Exception
    {
        if ( !graphAggregatorSet )
        {
            builder.withDiscoverer( discoverer );
            setAggregator( getAggregator() );
        }
        else
        {
            throw new IllegalStateException( "Aggregator already initialized!" );
        }
    }

    public void setAggregator( final GraphAggregator aggregator )
        throws Exception
    {
        builder.withGraphAggregator( aggregator );
        graphAggregatorSet = true;
        cartographer();
    }

    @Override
    public void before()
        throws Exception
    {
        cartographer();
    }

    @Override
    public void after()
    {
        try
        {
            if ( cartographer != null )
            {
                cartographer.close();
                cartographer = null;
            }
        }
        catch ( final CartoDataException e )
        {
            e.printStackTrace();
        }

        super.after();
    }

    public CartographerCore cartographer()
        throws Exception
    {
        if ( cartographer == null )
        {
            initCartographer();
            cartographer = builder.build();
        }

        return cartographer;
    }

    public RelationshipGraph openGraph( final ViewParams params, final boolean create )
        throws Exception
    {
        return cartographer().getGraphFactory()
                             .open( params, create );
    }

    public MavenModelProcessor getModelProcessor()
    {
        return new MavenModelProcessor();
    }

    public TestTransport getTransport()
    {
        return testTransport;
    }

    public TypeMapper getMapper()
        throws Exception
    {
        return cartographer().getGalley()
                             .getTypeMapper();
    }

    public ArtifactManager getArtifacts()
        throws Exception
    {
        return cartographer().getGalley()
                             .getArtifactManager();
    }

    public MavenPomReader getPomReader()
        throws Exception
    {
        return cartographer().getGalley()
                             .getPomReader();
    }

    public GraphResolver getGraphResolver()
                    throws Exception
    {
        return cartographer().getGraphResolver();
    }
}
