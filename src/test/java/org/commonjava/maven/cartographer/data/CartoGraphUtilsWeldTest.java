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
package org.commonjava.maven.cartographer.data;

import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class CartoGraphUtilsWeldTest
    extends AbstractCartoGraphUtilsTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Weld weld;

    private WeldContainer container;

    private GraphAggregator aggregator;

    private RelationshipGraphFactory graphFactory;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Override
    protected void setupComponents()
    {
        weld = new Weld();

        //        weld.addExtension( new CoreCDIExtension().withDefaultComponentInstances()
        //                                                 .withDefaultBeans()
        //                                                 .withDefaultBean( new EGraphManager( new FileNeo4JEGraphDriver( temp.newFolder( "db" ) ) ),
        //                                                                   EGraphManager.class )
        //                                                 .withDefaultBean( new MemoryPasswordManager(), PasswordManager.class )
        //                                                 .withDefaultBean( new FileCacheProviderConfig( temp.newFolder( "cache" ) ),
        //                                                                   FileCacheProviderConfig.class ) );
        container = weld.initialize();

        logger.info( "Selecting components from weld..." );
        graphFactory = container.instance()
                                .select( RelationshipGraphFactory.class )
                                .get();

        aggregator = container.instance()
                              .select( GraphAggregator.class )
                              .get();
    }

    @Override
    protected RelationshipGraphFactory getGraphFactory()
    {
        return graphFactory;
    }

    @After
    public void teardownDB()
        throws Exception
    {
        if ( weld != null && container != null )
        {
            weld.shutdown();
        }
    }

    @Override
    protected GraphAggregator getAggregator()
        throws Exception
    {
        return aggregator;
    }

}
