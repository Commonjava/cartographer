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
package org.commonjava.maven.cartographer.preset;

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
