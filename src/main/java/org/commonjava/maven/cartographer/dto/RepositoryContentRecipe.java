package org.commonjava.maven.cartographer.dto;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.galley.model.Location;

public class RepositoryContentRecipe
{
    private static final Set<String> DEFAULT_METAS = new HashSet<String>()
    {
        private static final long serialVersionUID = 1L;

        {
            add( "sha1" );
            add( "md5" );
            add( "asc" );
        }
    };

    private GraphComposition graphComposition;

    private Set<String> patcherIds;

    private Set<ExtraCT> extras;

    private String workspaceId;

    private Location sourceLocation;

    private Set<Location> excludedSourceLocations;

    private boolean multiSourceGAVs;

    private Integer timeoutSecs;

    private Set<String> metas;

    public String getWorkspaceId()
    {
        return workspaceId;
    }

    public Location getSourceLocation()
    {
        return sourceLocation;
    }

    public void setWorkspaceId( final String workspaceId )
    {
        this.workspaceId = workspaceId;
    }

    public void setSourceLocation( final Location source )
    {
        this.sourceLocation = source;
    }

    @Override
    public String toString()
    {
        return String.format( "RepositoryContentRecipe [graphs=%s, workspaceId=%s, source-location=%s]", graphComposition, workspaceId,
                              getSourceLocation() );
    }

    public boolean isValid()
    {
        return getWorkspaceId() != null && getSourceLocation() != null && graphComposition != null && graphComposition.isValid();
    }

    public DiscoveryConfig getDiscoveryConfig()
        throws URISyntaxException
    {
        final DefaultDiscoveryConfig ddc = new DefaultDiscoveryConfig( getSourceLocation().toString() );
        ddc.setEnabled( true );

        return ddc;
    }

    public Set<ExtraCT> getExtras()
    {
        return extras;
    }

    public void setExtras( final Set<ExtraCT> extras )
    {
        this.extras = extras;
    }

    public Integer getTimeoutSecs()
    {
        return timeoutSecs == null ? 10 : timeoutSecs;
    }

    public void setTimeoutSecs( final Integer timeoutSecs )
    {
        this.timeoutSecs = timeoutSecs;
    }

    public Set<String> getMetas()
    {
        return metas == null ? DEFAULT_METAS : metas;
    }

    public void setMetas( final Set<String> metas )
    {
        this.metas = metas;
    }

    public Set<Location> getExcludedSourceLocations()
    {
        return excludedSourceLocations;
    }

    public void setExcludedSourceLocations( final Set<Location> excludedSourceLocations )
    {
        this.excludedSourceLocations = excludedSourceLocations;
    }

    public boolean hasWildcardExtras()
    {
        if ( extras != null )
        {
            for ( final ExtraCT extra : extras )
            {
                if ( ExtraCT.WILDCARD.equals( extra.getClassifier() ) || ExtraCT.WILDCARD.equals( extra.getType() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    public Set<String> getPatcherIds()
    {
        return patcherIds;
    }

    public void setPatcherIds( final Set<String> patcherIds )
    {
        this.patcherIds = patcherIds;
    }

    public boolean isMultiSourceGAVs()
    {
        return multiSourceGAVs;
    }

    public void setMultiSourceGAVs( final boolean multiSourceGAVs )
    {
        this.multiSourceGAVs = multiSourceGAVs;
    }

    public GraphComposition getGraphComposition()
    {
        return graphComposition;
    }

    public void setGraphComposition( final GraphComposition graphComposition )
    {
        this.graphComposition = graphComposition;
    }

    public void resolveFilters( final PresetSelector presets, final String defaultPreset )
    {
        graphComposition.resolveFilters( presets, defaultPreset );
    }

    public void normalize()
    {
        graphComposition.normalize();
        normalize( patcherIds );
        normalize( extras );
        normalize( metas );
        normalize( excludedSourceLocations );
    }

    private void normalize( final Collection<?> coll )
    {
        if ( coll == null )
        {
            return;
        }

        for ( final Iterator<?> it = coll.iterator(); it.hasNext(); )
        {
            if ( it.next() == null )
            {
                it.remove();
            }
        }
    }

    private boolean resolve;

    public boolean isResolve()
    {
        return resolve;
    }

    public void setResolve( final boolean resolve )
    {
        this.resolve = resolve;
    }

}
