/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.maven.cartographer.data;

import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;

public class CartoDataManagerWeldTest
    extends AbstractCartoDataManagerTest
{

    private Weld weld;

    private WeldContainer container;

    private CartoDataManager dataManager;

    private GraphAggregator aggregator;

    private GraphWorkspaceHolder sessionManager;

    @Override
    protected void setupComponents()
    {
        weld = new Weld();
        container = weld.initialize();

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
