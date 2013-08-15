package org.commonjava.maven.cartographer.ops;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.preset.WorkspaceRecorder;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class ResolveOps
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    private GraphAggregator aggregator;

    @Inject
    private CartoDataManager data;

    protected ResolveOps()
    {
    }

    public ResolveOps( final CartoDataManager data, final DiscoverySourceManager sourceManager,
                       final ProjectRelationshipDiscoverer discoverer, final GraphAggregator aggregator )
    {
        this.data = data;
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregator = aggregator;
    }

    public List<ProjectVersionRef> resolveGraph( final String fromUri, final AggregationOptions options,
                                                 final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final URI source = sourceManager.createSourceURI( fromUri );
        if ( source == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.", fromUri,
                                          sourceManager.getFormatHint() );
        }

        sourceManager.activateWorkspaceSources( data.getCurrentWorkspace(), fromUri );

        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        final List<ProjectVersionRef> results = new ArrayList<>();
        for ( final ProjectVersionRef root : roots )
        {
            final DiscoveryResult result = discoverer.discoverRelationships( root, config );
            if ( result != null && data.contains( result.getSelectedRef() ) )
            {
                final ProjectVersionRef selected = result.getSelectedRef();

                final ProjectRelationshipFilter filter = options.getFilter();
                final EProjectGraph graph = data.getProjectGraph( filter, selected );
                if ( options.isDiscoveryEnabled() )
                {
                    aggregator.connectIncomplete( graph, options );
                }

                if ( filter != null && ( filter instanceof WorkspaceRecorder ) )
                {
                    ( (WorkspaceRecorder) filter ).save( data.getCurrentWorkspace() );
                }

                results.add( result.getSelectedRef() );
            }
        }

        return results;
    }

    public Set<ProjectVersionRef> resolveIncomplete( final String fromUri, final AggregationOptions options,
                                                     final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final URI source = sourceManager.createSourceURI( fromUri );
        if ( source == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.", fromUri,
                                          sourceManager.getFormatHint() );
        }

        sourceManager.activateWorkspaceSources( data.getCurrentWorkspace(), fromUri );

        final Set<ProjectVersionRef> seen = new HashSet<>();
        final Set<ProjectVersionRef> resolved = new HashSet<>();
        for ( final ProjectVersionRef root : roots )
        {
            int changed;
            do
            {
                final Set<ProjectVersionRef> incomplete = data.getIncompleteSubgraphsFor( options.getFilter(), root );
                incomplete.removeAll( seen );

                changed = 0;
                if ( incomplete != null && !incomplete.isEmpty() )
                {
                    for ( final ProjectVersionRef r : incomplete )
                    {
                        if ( seen.contains( r ) )
                        {
                            continue;
                        }

                        changed++;
                        try
                        {
                            final DiscoveryResult result =
                                discoverer.discoverRelationships( r, options.getDiscoveryConfig() );

                            if ( result != null )
                            {
                                resolved.add( result.getSelectedRef() );
                                seen.add( result.getSelectedRef() );
                            }
                        }
                        catch ( final CartoDataException e )
                        {
                            logger.warn( "%s: ERROR %s\n", r, e.getMessage() );
                        }

                        seen.add( r );
                    }
                }
            }
            while ( options.isDiscoveryEnabled() && changed > 0 );
        }

        return resolved;
    }

}
