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

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MutatorSelector
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<MutatorFactory> mutatorFactoryInstances;

    private static Map<String, MutatorFactory> mutatorFactories;

    private static final String DEFAULT_MUTATOR_ID = ManagedDependencyMutator.class.getSimpleName().toLowerCase();

    public MutatorSelector()
    {
        final ServiceLoader<MutatorFactory> factories = ServiceLoader.load( MutatorFactory.class );
        mapMutators( factories );
    }

    public MutatorSelector( final Iterable<MutatorFactory> mutatorFactoryInstances )
    {
        mapMutators( mutatorFactoryInstances );
    }

    @PostConstruct
    public void mapPresets()
    {
        mapMutators( mutatorFactoryInstances );
    }

    private void mapMutators( final Iterable<MutatorFactory> mutatorFactoryInstances )
    {
        mutatorFactories = new HashMap<String, MutatorFactory>();
        for ( final MutatorFactory factory : mutatorFactoryInstances )
        {
            final String[] named = factory.getMutatorIds();
            if ( named != null )
            {
                for ( final String name : named )
                {
                    logger.info( "Loaded mutator factory: {} ({})", name, factory );
                    mutatorFactories.put( name.toLowerCase(), factory );
                }
            }
            else
            {
                logger.info( "Skipped unnamed mutator factory: {}", factory );
            }
        }
    }

    public GraphMutator getGraphMutator( final String mutator )
    {
        if ( mutator == null )
        {
            return getDefaultMutator();
        }
        else
        {
            final MutatorFactory factory = mutatorFactories.get( mutator.toLowerCase() );
            if ( factory == null )
            {
                // TODO: Is there a more elegant way to handle this?
                throw new IllegalArgumentException( "Invalid mutator: " + mutator );
            }

            final GraphMutator mutatorInstance = factory.newMutator( mutator );
            
            logger.info( "Returning mutator: {} for ID: {}", mutatorInstance, mutator );
            return mutatorInstance;
        }
    }

    public static GraphMutator getDefaultMutator()
    {
        return mutatorFactories.get( DEFAULT_MUTATOR_ID ).newMutator( DEFAULT_MUTATOR_ID );
    }

}
