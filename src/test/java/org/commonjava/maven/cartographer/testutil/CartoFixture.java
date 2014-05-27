/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.testutil;

import java.util.concurrent.Executors;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.cartographer.Cartographer;
import org.commonjava.maven.cartographer.CartographerBuilder;
import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;

public class CartoFixture
    extends GalleyMavenFixture
{

    private CartographerBuilder builder;

    private Cartographer cartographer;

    private boolean graphAggregatorSet;

    public CartoFixture( final CoreFixture core )
    {
        super( core );
    }

    protected void initCartographer()
        throws Exception
    {
        if ( builder == null )
        {
            final RelationshipGraphConnectionFactory connectionFactory =
                new FileNeo4jConnectionFactory( getTemp().newFolder( "graph.", ".db" ), false );

            builder = new CartographerBuilder( getTemp().newFolder( "resolver.", ".dir" ), 1, connectionFactory );

            builder.withTransport( getTransport() );
            builder.withDiscoverer( new TestAggregatorDiscoverer() );
            if ( !graphAggregatorSet )
            {
                builder.withGraphAggregator( newGraphAggregator( builder.getDiscoverer() ) );
            }
        }

        cartographer = builder.build();
    }

    private GraphAggregator newGraphAggregator( final ProjectRelationshipDiscoverer discoverer )
    {
        return new DefaultGraphAggregator( discoverer, Executors.newFixedThreadPool( 1 ) );
    }

    @Override
    public void initMissingComponents()
        throws Exception
    {
        super.initMissingComponents();
        initCartographer();

        cartographer = builder.build();
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
        builder.withDiscoverer( discoverer );
        if ( !graphAggregatorSet )
        {
            builder.withGraphAggregator( newGraphAggregator( builder.getDiscoverer() ) );
        }

        if ( cartographer != null )
        {
            cartographer = builder.build();
        }
    }

    public void setAggregator( final GraphAggregator aggregator )
        throws CartoDataException
    {
        builder.withGraphAggregator( aggregator );
        graphAggregatorSet = true;
        if ( cartographer != null )
        {
            cartographer = builder.build();
        }
    }

    @Override
    public void after()
    {
        try
        {
            if ( cartographer != null )
            {
                cartographer.close();
            }
        }
        catch ( final CartoDataException e )
        {
            e.printStackTrace();
        }

        super.after();
    }

    public Cartographer cartographer()
        throws Exception
    {
        if ( cartographer == null )
        {
            initMissingComponents();
            initCartographer();
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

}
