package org.commonjava.maven.cartographer.agg;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class DefaultGraphAggregator
    implements GraphAggregator
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    @ExecutorConfig( daemon = true, named = "carto-aggregator", priority = 9, threads = 2 )
    private ExecutorService executor;

    protected DefaultGraphAggregator()
    {
    }

    public DefaultGraphAggregator( final CartoDataManager dataManager, final ProjectRelationshipDiscoverer discoverer,
                                   final ExecutorService executor )
    {
        this.dataManager = dataManager;
        this.discoverer = discoverer;
        this.executor = executor;
    }

    @Override
    public EProjectGraph connectIncomplete( final EProjectGraph graph, final AggregationOptions config )
        throws CartoDataException
    {
        return connect( graph, config, graph.getRoot() );
    }

    @Override
    public EProjectWeb connectIncomplete( final EProjectWeb web, final AggregationOptions config,
                                          final ProjectVersionRef... roots )
        throws CartoDataException
    {
        return connect( web, config, roots );
    }

    private <T extends EProjectNet> T connect( final T net, final AggregationOptions config,
                                               final ProjectVersionRef... roots )
        throws CartoDataException
    {
        if ( net != null )
        {
            if ( config.isDiscoveryEnabled() )
            {
                final LinkedList<EProjectNet> nets = new LinkedList<EProjectNet>();
                nets.add( net );

                final Set<ProjectVersionRef> missing = new HashSet<ProjectVersionRef>();
                final Set<ProjectVersionRef> cycleParticipants = loadExistingCycleParticipants( net );

                final LinkedList<DiscoveryTodo> pending = loadInitialPending( net );
                while ( !pending.isEmpty() )
                {
                    final HashSet<DiscoveryTodo> current = new HashSet<>( pending );
                    pending.clear();

                    final Set<DiscoveryTodo> newTodos = discover( current, config, cycleParticipants, missing );

                    for ( final DiscoveryTodo newTodo : newTodos )
                    {
                        if ( !pending.contains( newTodo ) )
                        {
                            pending.addLast( newTodo );
                        }
                    }
                }
            }
        }

        return net;
    }

    private Set<DiscoveryTodo> discover( final Set<DiscoveryTodo> todos, final AggregationOptions config,
                                         final Set<ProjectVersionRef> cycleParticipants,
                                         final Set<ProjectVersionRef> missing )
        throws CartoDataException
    {
        logger.info( "Performing discovery and cycle-detection on %d missing subgraphs: %s", todos.size(),
                     join( todos, ", " ) );

        final Set<DiscoveryRunnable> runnables = new HashSet<DiscoveryRunnable>( todos.size() );
        final CountDownLatch latch = new CountDownLatch( todos.size() );

        final Set<ProjectVersionRef> roMissing = Collections.unmodifiableSet( missing );
        for ( final DiscoveryTodo todo : todos )
        {
            final ProjectVersionRef todoRef = todo.getRef();

            if ( missing.contains( todoRef ) || cycleParticipants.contains( todoRef ) || dataManager.contains( todoRef ) )
            {
                continue;
            }

            logger.info( "Creating discovery runnable for: %s", todo );
            final DiscoveryRunnable runnable = new DiscoveryRunnable( todo, /*graph, graphs, cycleBuilders,*/config, /*cycleParticipants,*/
            roMissing, discoverer, dataManager, latch );

            executor.execute( runnable );
            runnables.add( runnable );
        }

        try
        {
            latch.await();
        }
        catch ( final InterruptedException e )
        {
            logger.error( "Interrupted on subgraph discovery." );
        }

        logger.info( "Accounting for discovery results. Before discovery, these were missing:\n\n%s\n\n", missing );

        final Set<DiscoveryTodo> newTodos = new HashSet<>();
        for ( final DiscoveryRunnable r : runnables )
        {
            missing.addAll( r.getNewMissing() );
            final Set<DiscoveryTodo> ntd = r.getNewTodos();
            if ( ntd != null )
            {
                newTodos.addAll( ntd );
            }
        }

        logger.info( "After discovery, these are missing:\n\n%s\n\n", missing );

        return newTodos;
    }

    private Set<ProjectVersionRef> loadExistingCycleParticipants( final EProjectNet net )
    {
        final Set<ProjectVersionRef> participants = new HashSet<>();
        final Set<EProjectCycle> cycles = net.getCycles();
        for ( final EProjectCycle cycle : cycles )
        {
            participants.addAll( cycle.getAllParticipatingProjects() );
        }

        return participants;
    }

    private LinkedList<DiscoveryTodo> loadInitialPending( final EProjectNet net )
    {
        final GraphView view = net.getView();
        final ProjectRelationshipFilter topFilter = view.getFilter();

        final LinkedList<DiscoveryTodo> initialPending = new LinkedList<>();
        final Set<ProjectVersionRef> initialIncomplete = net.getIncompleteSubgraphs();
        for ( final ProjectVersionRef ref : initialIncomplete )
        {
            final DiscoveryTodo todo = new DiscoveryTodo( ref );
            if ( initialPending.contains( todo ) )
            {
                continue;
            }

            final Set<ProjectRelationshipFilter> pathFilters = new HashSet<>();
            final Set<List<ProjectRelationship<?>>> paths = net.getPathsTo( ref );
            nextPath: for ( final List<ProjectRelationship<?>> path : paths )
            {
                ProjectRelationshipFilter f = topFilter;
                for ( final ProjectRelationship<?> rel : path )
                {
                    if ( !f.accept( rel ) )
                    {
                        continue nextPath;
                    }

                    f = f.getChildFilter( rel );
                }

                pathFilters.add( f );
            }

            if ( pathFilters.isEmpty() )
            {
                continue;
            }

            todo.setFilter( new OrFilter( pathFilters ) );
            initialPending.add( todo );
        }

        return initialPending;
    }

}
