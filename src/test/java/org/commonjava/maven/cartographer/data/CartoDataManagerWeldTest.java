/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.cartographer.data;

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
public class CartoDataManagerWeldTest
    extends AbstractCartoDataManagerTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Weld weld;

    private WeldContainer container;

    private CartoDataManager dataManager;

    private GraphAggregator aggregator;

    private GraphWorkspaceHolder sessionManager;

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
        sessionManager = container.instance()
                                  .select( GraphWorkspaceHolder.class )
                                  .get();

        dataManager = container.instance()
                               .select( CartoDataManager.class )
                               .get();

        aggregator = container.instance()
                              .select( GraphAggregator.class )
                              .get();
    }

    @Override
    protected GraphWorkspaceHolder getSessionManager()
    {
        return sessionManager;
    }

    @Override
    protected CartoDataManager getDataManager()
        throws Exception
    {
        return dataManager;
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
