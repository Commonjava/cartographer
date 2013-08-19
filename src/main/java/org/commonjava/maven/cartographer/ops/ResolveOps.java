package org.commonjava.maven.cartographer.ops;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
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

@ApplicationScoped
public class ResolveOps
{

    //    private final Logger logger = new Logger( getClass() );

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

    public List<ProjectVersionRef> resolve( final String fromUri, final AggregationOptions options,
                                            final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final URI source = sourceManager.createSourceURI( fromUri );
        if ( source == null )
        {
            throw new CartoDataException( "Invalid source format: '%s'. Use the form: '%s' instead.", fromUri,
                                          sourceManager.getFormatHint() );
        }

        GraphWorkspace ws = data.getCurrentWorkspace();
        if ( ws == null )
        {
            ws = data.createTemporaryWorkspace( new GraphWorkspaceConfiguration() );
        }

        sourceManager.activateWorkspaceSources( data.getCurrentWorkspace(), fromUri );

        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        final List<ProjectVersionRef> results = new ArrayList<>();
        for ( final ProjectVersionRef root : roots )
        {
            final ProjectVersionRef specific = discoverer.resolveSpecificVersion( root, config );
            if ( !data.contains( specific ) )
            {
                final DiscoveryResult result = discoverer.discoverRelationships( specific, config );
                if ( result != null && data.contains( result.getSelectedRef() ) )
                {
                    results.add( result.getSelectedRef() );
                }
            }
            else
            {
                results.add( specific );
            }
        }

        final ProjectRelationshipFilter filter = options.getFilter();
        final EProjectWeb web = data.getProjectWeb( filter, results.toArray( new ProjectVersionRef[results.size()] ) );
        if ( options.isDiscoveryEnabled() )
        {
            aggregator.connectIncomplete( web, options );
        }

        if ( filter != null && ( filter instanceof WorkspaceRecorder ) )
        {
            ( (WorkspaceRecorder) filter ).save( data.getCurrentWorkspace() );
        }

        return results;
    }

}
