package org.commonjava.maven.cartographer.dto;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
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

    private Set<ProjectVersionRef> roots;

    private Set<ExtraCT> extras;

    private String workspaceId;

    private Location sourceLocation;

    private Set<Location> excludedSourceLocations;

    private boolean resolve;

    private Integer timeoutSecs;

    private Set<String> metas;

    private ProjectRelationshipFilter filter;

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public String getWorkspaceId()
    {
        return workspaceId;
    }

    public Location getSourceLocation()
    {
        return sourceLocation;
    }

    public void setRoots( final Set<ProjectVersionRef> roots )
    {
        this.roots = roots;
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
        return String.format( "RepositoryContentRecipe [roots=%s, workspaceId=%s, filter=%s, source-location=%s]",
                              roots, workspaceId, getFilter(), getSourceLocation() );
    }

    public boolean isValid()
    {
        return getSourceLocation() != null && roots != null && !roots.isEmpty();
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

    public boolean isResolve()
    {
        return resolve;
    }

    public void setResolve( final boolean resolve )
    {
        this.resolve = resolve;
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

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public void setFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
    }

    public Set<Location> getExcludedSourceLocations()
    {
        return excludedSourceLocations;
    }

    public void setExcludedSourceLocations( final Set<Location> excludedSourceLocations )
    {
        this.excludedSourceLocations = excludedSourceLocations;
    }
}
