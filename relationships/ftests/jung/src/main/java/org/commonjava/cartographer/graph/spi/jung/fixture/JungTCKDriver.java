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
package org.commonjava.cartographer.graph.spi.jung.fixture;

import org.commonjava.cartographer.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.cartographer.graph.spi.jung.JungGraphConnectionFactory;
import org.commonjava.cartographer.tck.graph.testutil.TCKDriver;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * Created by jdcasey on 8/24/15.
 */
public class JungTCKDriver
        implements TCKDriver
{
    private TemporaryFolder temp;

    private JungGraphConnectionFactory factory;

    @Override
    public void setup( TemporaryFolder temp )
            throws Exception
    {
        this.temp = temp;
    }

    @Override
    public RelationshipGraphConnectionFactory getConnectionFactory()
            throws Exception
    {
        if ( factory == null )
        {
            factory = new JungGraphConnectionFactory();
        }

        return factory;
    }

    @Override
    public void close()
            throws IOException
    {
        if ( factory != null )
        {
            factory.close();
        }
    }
}
