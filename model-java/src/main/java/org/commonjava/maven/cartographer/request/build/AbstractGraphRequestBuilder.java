package org.commonjava.maven.cartographer.request.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.request.AbstractGraphRequest;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGraphRequestBuilder<O extends GraphRequestOwner<O, R>, T extends AbstractGraphRequestBuilder<O, T, R>, R extends AbstractGraphRequest>
{

    public static class StandaloneRequestOwner<R extends AbstractGraphRequest>
        implements GraphRequestOwner<StandaloneRequestOwner<R>, R>
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private R recipe;

        @Override
        public StandaloneRequestOwner<R> withGraphRequest( final R recipe )
        {
            logger.debug( "Got request: {}", recipe );
            this.recipe = recipe;
            return this;
        }

        public R getRecipe()
        {
            return recipe;
        }
    }

    protected String workspaceId;

    protected List<String> patcherIds;

    protected Integer timeoutSecs;

    protected String source;

    protected boolean resolve;

    protected transient Location sourceLocation;

    protected List<ProjectVersionRef> injectedBOMs;

    protected Map<ProjectRef, ProjectVersionRef> versionSelections;

    protected List<ProjectVersionRef> excludedSubgraphs;

    protected final T self;

    protected final O owner;

    @SuppressWarnings( "unchecked" )
    public AbstractGraphRequestBuilder( final O owner )
    {
        this.owner = owner;
        self = (T) this;
    }

    public abstract R build();

    public String getSource()
    {
        return source;
    }

    public T withSource( final String source )
    {
        this.source = source;
        return self;
    }

    public String getWorkspaceId()
    {
        return workspaceId;
    }

    public Location getSourceLocation()
    {
        return sourceLocation;
    }

    public T withWorkspaceId( final String workspaceId )
    {
        this.workspaceId = workspaceId;
        return self;
    }

    public T withSourceLocation( final Location source )
    {
        this.sourceLocation = source;
        this.source = sourceLocation.getUri();
        return self;
    }

    public Integer getTimeoutSecs()
    {
        return timeoutSecs == null ? 10 : timeoutSecs;
    }

    public T withTimeoutSecs( final Integer timeoutSecs )
    {
        this.timeoutSecs = timeoutSecs;
        return self;
    }

    public List<String> getPatcherIds()
    {
        return patcherIds;
    }

    public T withPatcherIds( final Collection<String> patcherIds )
    {
        this.patcherIds = new ArrayList<>();
        for ( final String id : patcherIds )
        {
            if ( !this.patcherIds.contains( id ) )
            {
                this.patcherIds.add( id );
            }
        }
        return self;
    }

    public boolean isResolve()
    {
        return resolve;
    }

    public T withResolve( final boolean resolve )
    {
        this.resolve = resolve;
        return self;
    }

    public List<ProjectVersionRef> getInjectedBOMs()
    {
        return injectedBOMs;
    }

    public T withInjectedBOMs( final List<ProjectVersionRef> injectedBOMs )
    {
        this.injectedBOMs = injectedBOMs;
        return self;
    }

    public List<ProjectVersionRef> getExcludedSubgraphs()
    {
        return excludedSubgraphs;
    }

    public T withExcludedSubgraphs( final Collection<ProjectVersionRef> excludedSubgraphs )
    {
        this.excludedSubgraphs = new ArrayList<ProjectVersionRef>( excludedSubgraphs );
        return self;
    }

    public Map<ProjectRef, ProjectVersionRef> getVersionSelections()
    {
        return versionSelections == null ? new HashMap<ProjectRef, ProjectVersionRef>() : versionSelections;
    }

    public T withVersionSelections( final Map<ProjectRef, ProjectVersionRef> versionSelections )
    {
        this.versionSelections = versionSelections;
        return self;
    }

    protected void configure( final AbstractGraphRequest recipe )
    {
        recipe.setExcludedSubgraphs( excludedSubgraphs );
        recipe.setInjectedBOMs( injectedBOMs );
        recipe.setPatcherIds( patcherIds );
        recipe.setResolve( resolve );
        recipe.setSource( source );
        recipe.setSourceLocation( sourceLocation );
        recipe.setTimeoutSecs( timeoutSecs );
        recipe.setVersionSelections( versionSelections );
        recipe.setWorkspaceId( workspaceId );
    }

    public O finishRecipe()
    {
        if ( owner != null )
        {
            return owner.withGraphRequest( build() );
        }

        return null;
    }

}
