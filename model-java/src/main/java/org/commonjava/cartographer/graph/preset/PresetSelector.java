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
package org.commonjava.cartographer.graph.preset;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PresetSelector
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<PresetFactory> presetFactoryInstances;

    private Map<String, PresetFactory> presetFactories;

    public PresetSelector()
    {
        final ServiceLoader<PresetFactory> factories = ServiceLoader.load( PresetFactory.class );
        mapPresets( factories );
    }

    public PresetSelector( final Iterable<PresetFactory> presetFactoryInstances )
    {
        mapPresets( presetFactoryInstances );
    }

    @PostConstruct
    public void mapPresets()
    {
        mapPresets( presetFactoryInstances );
    }

    private void mapPresets( final Iterable<PresetFactory> presetFilters )
    {
        presetFactories = new HashMap<String, PresetFactory>();
        for ( final PresetFactory filter : presetFilters )
        {
            final String[] named = filter.getPresetIds();
            if ( named != null )
            {
                for ( final String name : named )
                {
                    logger.info( "Loaded preset filter: {} ({})", name, filter );
                    presetFactories.put( name, filter );
                }
            }
            else
            {
                logger.info( "Skipped unnamed preset: {}", filter );
            }
        }
    }

    // TODO: Allow preset like: 'scoped(scope=runtime,managed=true)' by parsing off the '(..)' stuff into the parameter map and using the rest as the key.
    public ProjectRelationshipFilter getPresetFilter( String preset, final String defaultPreset,
                                                      final Map<String, Object> params )
    {
        if ( preset == null )
        {
            preset = defaultPreset;
        }

        Map<String, Object> parameters = params;
        if ( params == null )
        {
            parameters = new HashMap<String, Object>();
        }
        else
        {
            CommonPresetParameters.coerce( parameters );
        }

        if ( preset != null )
        {
            final PresetFactory factory = presetFactories.get( preset );
            if ( factory != null )
            {
                final ProjectRelationshipFilter filter = factory.newFilter( preset, parameters );

                logger.info( "Returning filter: {} for preset: {}", filter, preset );
                return filter;
            }

            // TODO: Is there a more elegant way to handle this?
            throw new IllegalArgumentException( "Invalid preset: " + preset );
        }

        return null;
    }

}
