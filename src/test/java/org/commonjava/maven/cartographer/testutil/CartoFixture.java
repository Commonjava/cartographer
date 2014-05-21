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

import java.io.IOException;
import java.util.concurrent.Executors;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jWorkspaceFactory;
import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.discover.SourceManagerImpl;
import org.commonjava.maven.cartographer.event.NoOpCartoEventManager;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;

public class CartoFixture
    extends GalleyMavenFixture
{

    private EGraphManager graphs;

    private DefaultCartoDataManager data;

    private TestAggregatorDiscoverer discoverer;

    private DefaultGraphAggregator aggregator;

    private CalculationOps calculationOps;

    private ResolveOps resolveOps;

    private NoOpCartoEventManager cartoEvents;

    private GraphWorkspaceHolder wsHolder;

    private SourceManagerImpl sourceManager;

    private MavenModelProcessor modelProcessor;

    public CartoFixture( final CoreFixture core )
    {
        super( core );
    }

    @Override
    public void initMissingComponents()
    {
        super.initMissingComponents();

        if ( graphs == null )
        {
            graphs = new EGraphManager( new FileNeo4jWorkspaceFactory( getTemp().newFolder( "graph.db" ), false ) );
        }

        if ( cartoEvents == null )
        {
            cartoEvents = new NoOpCartoEventManager();
        }

        if ( wsHolder == null )
        {
            wsHolder = new GraphWorkspaceHolder();
        }

        if ( data == null )
        {
            data = new DefaultCartoDataManager( graphs, wsHolder, cartoEvents );
        }
        if ( discoverer == null )
        {
            discoverer = new TestAggregatorDiscoverer( data );
        }

        if ( aggregator == null )
        {
            aggregator = new DefaultGraphAggregator( data, discoverer, Executors.newFixedThreadPool( 2 ) );
        }

        if ( sourceManager == null )
        {
            sourceManager = new SourceManagerImpl();
        }

        if ( calculationOps == null )
        {
            calculationOps = new CalculationOps( data );
        }

        if ( resolveOps == null )
        {
            resolveOps =
                new ResolveOps( calculationOps, data, sourceManager, discoverer, aggregator, getArtifacts(),
                                Executors.newFixedThreadPool( 10 ) );
        }

        if ( modelProcessor == null )
        {
            modelProcessor = new MavenModelProcessor( getData() );
        }
    }

    public EGraphManager getGraphs()
    {
        return graphs;
    }

    public DefaultCartoDataManager getData()
    {
        return data;
    }

    public TestAggregatorDiscoverer getDiscoverer()
    {
        return discoverer;
    }

    public DefaultGraphAggregator getAggregator()
    {
        return aggregator;
    }

    public ResolveOps getResolveOps()
    {
        return resolveOps;
    }

    public void setGraphs( final EGraphManager graphs )
    {
        this.graphs = graphs;
    }

    public void setData( final DefaultCartoDataManager data )
    {
        this.data = data;
    }

    public void setDiscoverer( final TestAggregatorDiscoverer discoverer )
    {
        this.discoverer = discoverer;
    }

    public void setAggregator( final DefaultGraphAggregator aggregator )
    {
        this.aggregator = aggregator;
    }

    public void setResolveOps( final ResolveOps resolveOps )
    {
        this.resolveOps = resolveOps;
    }

    @Override
    public void after()
    {
        try
        {
            if ( graphs != null )
            {
                graphs.close();
            }
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
        }

        super.after();
    }

    public NoOpCartoEventManager getCartoEvents()
    {
        return cartoEvents;
    }

    public GraphWorkspaceHolder getWsHolder()
    {
        return wsHolder;
    }

    public SourceManagerImpl getSourceManager()
    {
        return sourceManager;
    }

    public void setCartoEvents( final NoOpCartoEventManager cartoEvents )
    {
        this.cartoEvents = cartoEvents;
    }

    public void setWsHolder( final GraphWorkspaceHolder wsHolder )
    {
        this.wsHolder = wsHolder;
    }

    public void setSourceManager( final SourceManagerImpl sourceManager )
    {
        this.sourceManager = sourceManager;
    }

    public CalculationOps getCalculationOps()
    {
        return calculationOps;
    }

    public void setCalculationOps( final CalculationOps calculationOps )
    {
        this.calculationOps = calculationOps;
    }

    public MavenModelProcessor getModelProcessor()
    {
        return modelProcessor;
    }

    public void setModelProcessor( final MavenModelProcessor modelProcessor )
    {
        this.modelProcessor = modelProcessor;
    }

}
