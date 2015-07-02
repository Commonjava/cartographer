package org.commonjava.maven.cartographer.dto.resolve;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.cartographer.dto.ResolverRecipe;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationResolver;

public class DTOResolver
{

    @Inject
    private LocationResolver resolver;

    @Inject
    private PresetSelector presets;

    protected DTOResolver()
    {
    }

    public DTOResolver( final LocationResolver resolver, final PresetSelector presets )
    {
        this.resolver = resolver;
        this.presets = presets;
    }

    public void resolve( final ResolverRecipe recipe )
        throws CartoDataException
    {
        if ( recipe == null )
        {
            return;
        }

        resolveSourceLocation( recipe );
        resolvePresets( recipe );

        if ( recipe instanceof RepositoryContentRecipe )
        {
            final RepositoryContentRecipe rcr = (RepositoryContentRecipe) recipe;
            final Set<String> excludedSources = rcr.getExcludedSources();
            final Set<Location> excludedLocations = resolveSourceLocationSet( excludedSources );
            rcr.setExcludedSourceLocations( excludedLocations );
        }
    }

    public void resolveSourceLocation( final ResolverRecipe recipe )
        throws CartoDataException
    {
        if ( recipe == null )
        {
            return;
        }

        final String spec = recipe.getSource();

        if ( spec == null )
        {
            return;
        }

        Location location;
        try
        {
            location = resolver.resolve( spec );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve location from spec: '%s'. Reason: %s", e, spec,
                                          e.getMessage() );
        }

        recipe.setSourceLocation( location );
    }

    public void resolvePresets( final GraphComposition graphs )
    {
        if ( graphs == null )
        {
            return;
        }

        for ( final GraphDescription graph : graphs )
        {
            resolvePresets( graph );
        }
    }

    public void resolvePresets( final GraphDescription graph )
    {
        if ( graph == null )
        {
            return;
        }

        if ( graph.filter() == null )
        {
            final ProjectRelationshipFilter filter =
                presets.getPresetFilter( graph.getPreset(), graph.getDefaultPreset(), graph.getPresetParams() );

            graph.setFilter( filter );
        }
    }

    public void resolvePresets( final ResolverRecipe recipe )
    {
        if ( recipe == null )
        {
            return;
        }

        final GraphComposition comp = recipe.getGraphComposition();
        resolvePresets( comp );
    }

    public Set<Location> resolveSourceLocationSet( final Set<String> specs )
        throws CartoDataException
    {
        final Set<Location> locations = new HashSet<Location>();
        if ( specs != null )
        {
            for ( final String spec : specs )
            {
                if ( spec == null )
                {
                    continue;
                }

                Location location;
                try
                {
                    location = resolver.resolve( spec );
                }
                catch ( final TransferException e )
                {
                    throw new CartoDataException( "Failed to resolve location from spec: '%s'. Reason: %s", e, spec,
                                                  e.getMessage() );
                }

                if ( location != null )
                {
                    locations.add( location );
                }
            }
        }

        return locations;
    }

    public List<Location> resolveSourceLocationList( final List<String> specs )
        throws CartoDataException
    {
        final List<Location> locations = new ArrayList<Location>();
        if ( specs != null )
        {
            for ( final String spec : specs )
            {
                if ( spec == null )
                {
                    continue;
                }

                Location location;
                try
                {
                    location = resolver.resolve( spec );
                }
                catch ( final TransferException e )
                {
                    throw new CartoDataException( "Failed to resolve location from spec: '%s'. Reason: %s", e, spec,
                                                  e.getMessage() );
                }

                if ( location != null )
                {
                    locations.add( location );
                }
            }
        }

        return locations;
    }

}
