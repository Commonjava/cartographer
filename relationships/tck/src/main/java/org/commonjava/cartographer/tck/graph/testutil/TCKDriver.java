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
package org.commonjava.cartographer.tck.graph.testutil;

import org.commonjava.cartographer.graph.spi.RelationshipGraphConnectionFactory;
import org.junit.rules.TemporaryFolder;

import java.io.Closeable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface TCKDriver extends Closeable
{

    void setup( TemporaryFolder temp )
            throws Exception;

    RelationshipGraphConnectionFactory getConnectionFactory()
            throws Exception;
}
