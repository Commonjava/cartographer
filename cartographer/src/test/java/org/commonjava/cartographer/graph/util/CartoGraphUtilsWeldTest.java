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
package org.commonjava.cartographer.graph.util;

import org.commonjava.cartographer.graph.RelationshipGraphFactory;
import org.commonjava.cartographer.spi.graph.agg.GraphAggregator;
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
