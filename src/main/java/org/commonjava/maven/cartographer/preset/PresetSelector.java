package org.commonjava.maven.cartographer.preset;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class PresetSelector
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private Instance<PresetFactory> presetFactoryInstances;

    @Inject
    private CartoDataManager carto;

    private Map<String, PresetFactory> presetFactories;

    protected PresetSelector()
    {
    }

    public PresetSelector( final CartoDataManager carto, final Iterable<PresetFactory> presetFactoryInstances )
    {
        this.carto = carto;
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
            final String named = filter.getPresetId();
            if ( named != null )
            {
                logger.info( "Loaded preset filter: %s (%s)", named, filter );
                presetFactories.put( named, filter );
            }
            else
            {
                logger.info( "Skipped unnamed preset: %s", filter );
            }
        }
    }

    public ProjectRelationshipFilter getPresetFilter( String preset, final String defaultPreset )
    {
        if ( preset == null )
        {
            preset = defaultPreset;
        }

        if ( preset != null )
        {
            final PresetFactory factory = presetFactories.get( preset );
            if ( factory != null )
            {
                final GraphWorkspace ws = carto.getCurrentWorkspace();
                final ProjectRelationshipFilter filter = factory.newFilter( ws );

                logger.info( "Returning filter: %s for preset: %s", filter, preset );
                return filter;
            }

            // TODO: Is there a more elegant way to handle this?
            throw new IllegalArgumentException( "Invalid preset: " + preset );
        }

        return null;
    }

}
