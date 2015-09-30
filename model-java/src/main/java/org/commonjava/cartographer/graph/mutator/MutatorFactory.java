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
package org.commonjava.cartographer.graph.mutator;

import org.commonjava.maven.atlas.graph.mutate.GraphMutator;

public interface MutatorFactory
{

    /**
     * @return array of mutator IDs created by this factory
     */
    String[] getMutatorIds();

    /**
     * Provides a mutator instance. In cases when it is possible and desired it returns always the same default
     * instance rather than creating a new one for each call.
     *
     * @param mutatorId the mutator ID
     * @return the mutator instance
     */
    GraphMutator newMutator( String mutatorId );

}
