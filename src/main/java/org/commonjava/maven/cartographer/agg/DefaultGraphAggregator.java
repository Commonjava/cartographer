package org.commonjava.maven.cartographer.agg;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
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
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
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

    public DefaultGraphAggregator( final CartoDataManager dataManager, final ProjectRelationshipDiscoverer discoverer, final ExecutorService executor )
    {
        this.dataManager = dataManager;
        this.discoverer = discoverer;
        this.executor = executor;
    }

    @Override
    public EProjectGraph connectIncomplete( final EProjectGraph graph, final AggregationOptions config )
        throws CartoDataException
    {
        return connect( graph, config );
    }

    @Override
    public EProjectWeb connectIncomplete( final EProjectWeb web, final AggregationOptions config )
        throws CartoDataException
    {
        return connect( web, config );
    }

    private <T extends EProjectNet> T connect( final T net, final AggregationOptions config )
        throws CartoDataException
    {
        if ( net != null )
        {
            if ( config.isDiscoveryEnabled() )
            {
                final LinkedList<EProjectNet> nets = new LinkedList<EProjectNet>();
                nets.add( net );

                final Set<ProjectVersionRef> missing = new HashSet<ProjectVersionRef>();

                logger.info( "Loading existing cycle participants..." );
                final Set<ProjectVersionRef> cycleParticipants = loadExistingCycleParticipants( net );

                logger.info( "Loading initial set of GAVs to be resolved..." );
                final LinkedList<DiscoveryTodo> pending = loadInitialPending( net );

                while ( !pending.isEmpty() )
                {
                    final HashSet<DiscoveryTodo> current = new HashSet<>( pending );
                    logger.info( "Next batch of TODOs: %s", current );
                    pending.clear();

                    final Set<DiscoveryTodo> newTodos = discover( current, config, cycleParticipants, missing, net );
                    if ( newTodos != null )
                    {
                        logger.info( "Uncovered new batch of TODOs: %s", newTodos );
                        for ( final DiscoveryTodo newTodo : newTodos )
                        {
                            // don't have to check contains for this new todo, since we cleared the pending list above...
                            pending.addLast( newTodo );
                        }
                    }
                }
            }
        }

        return net;
    }

    private Set<DiscoveryTodo> discover( final Set<DiscoveryTodo> todos, final AggregationOptions config,
                                         final Set<ProjectVersionRef> cycleParticipants, final Set<ProjectVersionRef> missing, final EProjectNet net )
        throws CartoDataException
    {
        logger.info( "Performing discovery and cycle-detection on %d missing subgraphs: %s", todos.size(), join( todos, ", " ) );

        final Set<DiscoveryRunnable> runnables = new HashSet<DiscoveryRunnable>( todos.size() );
        final CountDownLatch latch = new CountDownLatch( todos.size() );

        final Set<ProjectVersionRef> roMissing = Collections.unmodifiableSet( missing );
        for ( final DiscoveryTodo todo : todos )
        {
            final ProjectVersionRef todoRef = todo.getRef();

            if ( missing.contains( todoRef ) )
            {
                logger.info( "Skipping missing reference: %s", todoRef );
                latch.countDown(); // over-allocated, so reduce the count
                continue;
            }
            else if ( cycleParticipants.contains( todoRef ) )
            {
                logger.info( "Skipping cycle-participant reference: %s", todoRef );
                latch.countDown(); // over-allocated, so reduce the count
                continue;
            }
            else if ( net.containsGraph( todoRef ) )
            {
                logger.info( "Skipping already-discovered reference: %s", todoRef );
                latch.countDown(); // over-allocated, so reduce the count
                continue;
            }

            logger.info( "Creating discovery runnable for: %s", todo );
            final DiscoveryRunnable runnable = new DiscoveryRunnable( todo, config, roMissing, discoverer, false, latch );

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

        final Set<ProjectRelationship<?>> newRels = new HashSet<>();
        final Set<DiscoveryTodo> newTodos = new HashSet<>();
        for ( final DiscoveryRunnable r : runnables )
        {
            final DiscoveryResult result = r.getResult();
            final DiscoveryTodo todo = r.getTodo();

            if ( result != null )
            {
                final Set<ProjectRelationshipFilter> filters = todo.getFilters();

                final Set<ProjectRelationship<?>> discoveredRels = result.getAcceptedRelationships();
                if ( discoveredRels != null )
                {
                    newRels.addAll( discoveredRels );

                    for ( final ProjectRelationship<?> rel : newRels )
                    {
                        final ProjectVersionRef relTarget = rel.getTarget()
                                                               .asProjectVersionRef();
                        if ( !net.containsGraph( relTarget ) )
                        {
                            final Set<ProjectRelationshipFilter> acceptingChildren = new HashSet<>();
                            for ( final ProjectRelationshipFilter filter : filters )
                            {
                                if ( filter.accept( rel ) )
                                {
                                    acceptingChildren.add( filter.getChildFilter( rel ) );
                                }
                            }

                            if ( !acceptingChildren.isEmpty() )
                            {
                                newTodos.add( new DiscoveryTodo( relTarget, acceptingChildren ) );
                            }
                        }
                    }
                }
            }
            else
            {
                markMissing( todo.getRef(), todo, missing );
            }
        }

        if ( !newRels.isEmpty() )
        {
            final Set<ProjectRelationship<?>> rejected = dataManager.storeRelationships( newRels );
            addToCycleParticipants( rejected, cycleParticipants );
        }

        logger.info( "After discovery, these are missing:\n\n%s\n\n", missing );

        return newTodos;
    }

    private void addToCycleParticipants( final Set<ProjectRelationship<?>> rejectedRelationships, final Set<ProjectVersionRef> cycleParticipants )
    {
        for ( final ProjectRelationship<?> rejected : rejectedRelationships )
        {
            cycleParticipants.add( rejected.getDeclaring()
                                           .asProjectVersionRef() );
            cycleParticipants.add( rejected.getTarget()
                                           .asProjectVersionRef() );
        }
    }

    private void markMissing( final ProjectVersionRef ref, final DiscoveryTodo todo, final Set<ProjectVersionRef> missing )
    {
        missing.add( ref );
        final ProjectVersionRef originalRef = todo.getRef();
        if ( !originalRef.equals( ref ) )
        {
            missing.add( originalRef );
        }
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

        final Set<ProjectVersionRef> initialIncomplete = net.getIncompleteSubgraphs();

        logger.info( "Finding paths from: %s to: %s", net.getView()
                                                         .getRoots(), initialIncomplete );

        final Set<List<ProjectRelationship<?>>> paths = net.getPathsTo( initialIncomplete.toArray( new ProjectVersionRef[initialIncomplete.size()] ) );

        if ( paths == null || paths.isEmpty() )
        {
            return new LinkedList<>();
        }

        final Map<ProjectVersionRef, Set<ProjectRelationshipFilter>> filtersByRef = new HashMap<>();
        nextPath: for ( final List<ProjectRelationship<?>> path : paths )
        {
            if ( path == null || path.size() < 1 )
            {
                continue;
            }

            final ProjectVersionRef ref = path.get( path.size() - 1 )
                                              .getTarget()
                                              .asProjectVersionRef();
            Set<ProjectRelationshipFilter> pathFilters = filtersByRef.get( ref );
            if ( pathFilters == null )
            {
                pathFilters = new HashSet<>();
                filtersByRef.put( ref, pathFilters );
            }

            ProjectRelationshipFilter f = topFilter;
            for ( final ProjectRelationship<?> rel : path )
            {
                if ( !f.accept( rel ) )
                {
                    continue nextPath;
                }

                f = f.getChildFilter( rel );
            }

            logger.info( "Adding todo: %s via filter: %s", ref, f );
            pathFilters.add( f );
        }

        final LinkedList<DiscoveryTodo> initialPending = new LinkedList<>();
        for ( final Entry<ProjectVersionRef, Set<ProjectRelationshipFilter>> entry : filtersByRef.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final Set<ProjectRelationshipFilter> pathFilters = entry.getValue();

            final DiscoveryTodo todo = new DiscoveryTodo( ref );

            if ( pathFilters.isEmpty() )
            {
                continue;
            }

            todo.setFilters( pathFilters );
            initialPending.add( todo );
        }

        return initialPending;
    }

}
