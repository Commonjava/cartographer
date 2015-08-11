package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.cartographer.request.ExtraCT;
import org.commonjava.maven.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.galley.model.Location;

import java.util.Set;
import java.util.TreeSet;

public class RepositoryContentRequestBuilder<T extends RepositoryContentRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends RepositoryContentRequest>
    extends MultiGraphRequestBuilder<T, O, R>
{

    public static final class StandaloneRCRB
        extends RepositoryContentRequestBuilder<StandaloneRCRB, StandaloneRequestOwner<RepositoryContentRequest>, RepositoryContentRequest>
    {
        public StandaloneRCRB()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandaloneRCRB newRepositoryContentRecipeBuilder()
    {
        return new StandaloneRCRB();
    }

    public RepositoryContentRequestBuilder( final O owner )
    {
        super( owner );
    }

    private boolean multiSourceGAVs;

    private Set<ExtraCT> extras;

    private Set<String> metas;

    private Set<String> excludedSources;

    private transient Set<Location> excludedSourceLocations;

    private boolean localUrls;

    public Set<String> getExcludedSources()
    {
        return excludedSources;
    }

    public T withExcludedSources( final Set<String> excludedSources )
    {
        this.excludedSources = new TreeSet<String>( excludedSources );
        return self;
    }

    public Set<ExtraCT> getExtras()
    {
        return extras;
    }

    public T withExtras( final Set<ExtraCT> extras )
    {
        this.extras = new TreeSet<>( extras );
        return self;
    }

    public Set<String> getMetas()
    {
        return metas;
    }

    public T withMetas( final Set<String> metas )
    {
        this.metas = metas;
        return self;
    }

    public Set<Location> getExcludedSourceLocations()
    {
        return excludedSourceLocations;
    }

    public T withExcludedSourceLocations( final Set<Location> excludedSourceLocations )
    {
        this.excludedSourceLocations = excludedSourceLocations;
        return self;
    }

    public boolean isMultiSourceGAVs()
    {
        return multiSourceGAVs;
    }

    public T withMultiSourceGAVs( final boolean multiSourceGAVs )
    {
        this.multiSourceGAVs = multiSourceGAVs;
        return self;
    }

    public boolean getLocalUrls()
    {
        return localUrls;
    }

    public T withLocalUrls( final boolean localUrls )
    {
        this.localUrls = localUrls;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final RepositoryContentRequest recipe = new RepositoryContentRequest();
        configure( recipe );
        configureMultiGraphs( recipe );
        configureRepoContent( recipe );

        return (R) recipe;
    }

    protected void configureRepoContent( final RepositoryContentRequest recipe )
    {
        recipe.setMultiSourceGAVs( multiSourceGAVs );
        recipe.setExtras( extras );
        recipe.setMetas( metas );
        recipe.setExcludedSourceLocations( excludedSourceLocations );
        recipe.setExcludedSources( excludedSources );
        recipe.setLocalUrls( localUrls );
    }

}
