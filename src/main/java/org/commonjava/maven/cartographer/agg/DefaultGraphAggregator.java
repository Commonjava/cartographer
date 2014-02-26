/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.util.JoinString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DefaultGraphAggregator
    implements GraphAggregator
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    @ExecutorConfig( daemon = true, named = "carto-aggregator", priority = 9, threads = 8 )
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
    public EProjectNet connectIncomplete( final EProjectWeb web, final AggregationOptions config )
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

                logger.debug( "Loading existing cycle participants..." );
                final Set<ProjectVersionRef> cycleParticipants = loadExistingCycleParticipants( net );

                logger.debug( "Loading initial set of GAVs to be resolved..." );
                final LinkedList<DiscoveryTodo> pending = loadInitialPending( net, config.getMutator() );
                final HashSet<DiscoveryTodo> done = new HashSet<DiscoveryTodo>();

                int pass = 0;
                while ( !pending.isEmpty() )
                {
                    final HashSet<DiscoveryTodo> current = new HashSet<DiscoveryTodo>( pending );
                    done.addAll( current );

                    logger.info( "{}. Next batch of TODOs:\n  {}", pass, new JoinString( "\n  ", current ) );
                    pending.clear();

                    final Set<DiscoveryTodo> newTodos = discover( current, config, cycleParticipants, missing, net, pass );
                    if ( newTodos != null )
                    {
                        newTodos.removeAll( done );
                        logger.info( "{}. Uncovered new batch of TODOs:\n  {}", pass, new JoinString( "\n  ", newTodos ) );

                        pending.addAll( newTodos );
                    }

                    pass++;
                }
            }
        }

        return net;
    }

    private Set<DiscoveryTodo> discover( final Set<DiscoveryTodo> todos, final AggregationOptions config,
                                         final Set<ProjectVersionRef> cycleParticipants, final Set<ProjectVersionRef> missing, final EProjectNet net,
                                         final int pass )
        throws CartoDataException
    {
        logger.debug( "{}. Performing discovery and cycle-detection on {} missing subgraphs:\n  {}", pass, todos.size(), new JoinString( "\n  ",
                                                                                                                                         todos ) );

        final Set<DiscoveryRunnable> runnables = executeTodoBatch( todos, net, config, missing, cycleParticipants, pass );

        logger.info( "{}. Accounting for discovery results. Before discovery, these were missing:\n\n  {}\n\n", pass,
                     new JoinString( "\n  ", missing ) );

        final Set<ProjectRelationship<?>> toStore = new HashSet<ProjectRelationship<?>>();
        final Map<ProjectVersionRef, DiscoveryTodo> nextTodos = new HashMap<ProjectVersionRef, DiscoveryTodo>();

        for ( final DiscoveryRunnable r : runnables )
        {
            if ( !processDiscoveryOutput( r, toStore, nextTodos, net, config.getDiscoveryConfig(), pass ) )
            {
                markMissing( r, missing, pass );
            }
        }

        if ( !toStore.isEmpty() )
        {
            logger.info( "Storing relationships:\n\n  {}\n\n", join( toStore, "\n  " ) );
            final Set<ProjectRelationship<?>> rejected = dataManager.storeRelationships( toStore );

            logger.info( "Marking rejected relationships as cycle-injectors:\n  {}", join( rejected, "\n  " ) );
            addToCycleParticipants( rejected, cycleParticipants );
        }

        // this has to wait until the "vanilla" relationships are stored, since
        // it may depend on lookup of context relationships (managed deps, for instance)
        mutateNextTodos( nextTodos, pass );

        logger.info( "{}. After discovery, these are missing:\n\n  {}\n\n", pass, new JoinString( "\n  ", missing ) );

        return new HashSet<DiscoveryTodo>( nextTodos.values() );
    }

    private void mutateNextTodos( final Map<ProjectVersionRef, DiscoveryTodo> nextTodos, final int pass )
    {
        int index = 0;
        for ( ProjectVersionRef ref : new HashSet<ProjectVersionRef>( nextTodos.keySet() ) )
        {
            final DiscoveryTodo todo = nextTodos.remove( ref );

            final Set<GraphMutator> mutators = todo.getMutators();
            if ( mutators != null && !mutators.isEmpty() )
            {
                // For each mutation, add to DiscoveryTodo list, but DO NOT STORE 
                // (newRels determines what gets stored).
                //
                // Instead, allow the database to auto-create these so they're 
                // distinguishable from non-mutated relationships.
                int idx = 0;
                for ( final ProjectRelationship<?> rel : todo.getSourceRelationships() )
                {
                    for ( final GraphMutator mutator : mutators )
                    {
                        final ProjectRelationship<?> selected = mutator.selectFor( rel );
                        if ( selected != null && selected != rel )
                        {
                            logger.info( "{}\n\n  MUTATED TO:\n\n{}\n", rel, selected );

                            ref = selected.getTarget()
                                          .asProjectVersionRef();

                            logger.info( "{}.{}.{}. MUTATE-DISCOVER += {}\n  (filters:\n    {})", pass, index, idx, ref,
                                         new JoinString( "\n    ", todo.getFilters() ) );
                        }
                        else
                        {
                            logger.info( "{}.{}.{}. DISCOVER += {}\n  (filters:\n    {})", pass, index, idx, ref,
                                         new JoinString( "\n    ", todo.getFilters() ) );
                        }

                        final GraphMutator childMutator = mutator.getMutatorFor( selected );

                        incorporateTodoInfo( ref, todo.getFilters(), nextTodos, Collections.singleton( childMutator ) );
                    }

                    idx++;
                }
            }
            else
            {
                logger.info( "{}.{}. DISCOVER += {}\n  (filters:\n    {})", pass, index, ref, new JoinString( "\n    ", todo.getFilters() ) );

                nextTodos.put( ref, todo );
            }

            index++;
        }
    }

    /**
     * Convert the current set of {@link DiscoveryTodo}'s into a set of 
     * {@link DiscoveryRunnable}'s after first ensuring their corresponding GAVs 
     * aren't already present, listed as missing, or listed as participants in a 
     * relationship cycle.
     * 
     * Then, execute all of these runnables and wait for processing to complete
     * before passing them back for output processing.
     * 
     * @param todos The current set of {@link DiscoveryTodo}'s to process
     * @param net The top-level dependency graph for which discovery it taking place
     * @param config Configuration for how discovery should proceed
     * @param missing The accumulated list of confirmed-missing GAVs (NOT things 
     * that have yet to be discovered)
     * @param cycleParticipants The accumulated list of GAVs that participate in 
     * relationship cycles. These are NOT safe to continue processing, since they 
     * will lead to infinite looping.
     * @param pass For diagnostic/logging purposes, the number of discovery passes 
     * since discovery was initiated by the caller (part of the graph may have been 
     * pre-existing)
     * @return The executed set of {@link DiscoveryRunnable} instances that contain 
     * output to be processed and incorporated in the graph.
     */
    private Set<DiscoveryRunnable> executeTodoBatch( final Set<DiscoveryTodo> todos, final EProjectNet net, final AggregationOptions config,
                                                     final Set<ProjectVersionRef> missing, final Set<ProjectVersionRef> cycleParticipants,
                                                     final int pass )
    {
        final Set<DiscoveryRunnable> runnables = new HashSet<DiscoveryRunnable>( todos.size() );

        final Set<ProjectVersionRef> roMissing = Collections.unmodifiableSet( missing );
        int idx = 0;
        for ( final DiscoveryTodo todo : todos )
        {
            final ProjectVersionRef todoRef = todo.getRef();

            if ( missing.contains( todoRef ) )
            {
                logger.info( "{}.{}. Skipping missing reference: {}", pass, idx++, todoRef );
                continue;
            }
            else if ( cycleParticipants.contains( todoRef ) )
            {
                logger.info( "{}.{}. Skipping cycle-participant reference: {}", pass, idx++, todoRef );
                continue;
            }
            else if ( net.containsGraph( todoRef ) )
            {
                logger.info( "{}.{}. Skipping already-discovered reference: {}", pass, idx++, todoRef );
                continue;
            }

            //            logger.info( "DISCOVER += {}", todo );
            final DiscoveryRunnable runnable = new DiscoveryRunnable( todo, config, roMissing, discoverer, false, pass, idx );
            runnables.add( runnable );
            idx++;
        }

        final CountDownLatch latch = new CountDownLatch( runnables.size() );
        for ( final DiscoveryRunnable runnable : runnables )
        {
            runnable.setLatch( latch );
            executor.execute( runnable );
        }

        try
        {
            latch.await();
        }
        catch ( final InterruptedException e )
        {
            logger.error( "Interrupted on subgraph discovery." );
            return null;
        }

        return runnables;
    }

    /**
     * Process the output from a discovery runnable (discovery of relationships 
     * related to a given GAV). This includes:
     * 
     * <ul>
     *   <li>Adding any accumulated metadata for the GAV</li>
     *   <li>Determining which new relationships to store in the graph db related to the relationships in this result</li>
     *   <li>Generating the next set of {@link DiscoveryTodo}'s related to the relationships in this result</li>
     * </ul>
     * 
     * @param r The runnable containing discovery output to process for a specific 
     * input GAV
     * @param toStore The accumulated list of relationships that shoudl be stored 
     * in the graph db for this input GAV
     * @param nextTodos The accumulated next crop of {@link DiscoveryTodo}'s, which 
     * MAY be augmented by output from this discovery runnable 
     * @param net The top-level dependency graph for which discovery is taking place
     * @param config Configuration for how discovery should proceed
     * @param pass For diagnostic/logging purposes, the number of discovery passes 
     * since discovery was initiated by the caller (part of the graph may have been pre-existing)
     * @return true if output contained a valid result, or false to indicate the 
     * GAV should be marked missing.
     */
    private boolean processDiscoveryOutput( final DiscoveryRunnable r, final Set<ProjectRelationship<?>> toStore,
                                            final Map<ProjectVersionRef, DiscoveryTodo> nextTodos, final EProjectNet net,
                                            final DiscoveryConfig config, final int pass )
    {
        final DiscoveryResult result = r.getResult();

        if ( result != null )
        {
            final Map<String, String> metadata = result.getMetadata();

            if ( metadata != null )
            {
                dataManager.addMetadata( result.getSelectedRef(), metadata );
            }

            final Set<ProjectRelationship<?>> discoveredRels = result.getAcceptedRelationships();
            if ( discoveredRels != null )
            {
                final DiscoveryTodo todo = r.getTodo();
                final int index = r.getIndex();
                final Set<ProjectRelationshipFilter> filters = todo.getFilters();
                final Set<GraphMutator> mutators = todo.getMutators();

                // De-selected relationships (not mutated) should be stored but NOT followed for discovery purposes.
                // Likewise, mutated (selected) relationships should be followed but NOT stored.
                logger.info( "{}.{}. Processing {} new relationships for: {}\n\n  {}", pass, index, discoveredRels.size(), result.getSelectedRef(),
                             join( discoveredRels, "\n  " ) );

                boolean contributedRels = false;

                int idx = 0;
                for ( final ProjectRelationship<?> rel : discoveredRels )
                {
                    final ProjectVersionRef relTarget = rel.getTarget()
                                                           .asProjectVersionRef();
                    if ( !net.containsGraph( relTarget ) )
                    {
                        // TODO: We're filtering on the original relationship THEN potentially mutating it for the next 
                        // layer of discovery...is it wise not to filter those as well??
                        final Set<ProjectRelationshipFilter> nextFilters = findNextFilters( rel, filters, pass, index, idx );

                        if ( !nextFilters.isEmpty() )
                        {
                            contributedRels = true;
                            toStore.add( rel );
                            incorporateTodoInfo( relTarget, nextFilters, nextTodos, mutators );

                            final DiscoveryTodo nextTodo = nextTodos.get( relTarget );
                            Set<ProjectRelationship<?>> sourceRelationships = nextTodo.getSourceRelationships();
                            if ( sourceRelationships == null )
                            {
                                sourceRelationships = new HashSet<ProjectRelationship<?>>();
                                nextTodo.setSourceRelationships( sourceRelationships );
                            }

                            sourceRelationships.add( rel );
                        }
                        else if ( rel.isManaged() )
                        {
                            logger.info( "{}.{}.{}. FORCE; NON-TRAVERSE: Adding managed relationship (for mutator use later): {}", pass, index, idx,
                                         rel );
                            toStore.add( rel );
                            contributedRels = true;
                        }
                        else if ( rel.getType() == RelationshipType.PARENT )
                        {
                            logger.info( "{}.{}.{}. FORCE; NON-TRAVERSE: Adding parent relationship: {}", pass, index, idx, rel );
                            toStore.add( rel );
                            contributedRels = true;
                        }
                        else
                        {
                            logger.info( "{}.{}.{}. SKIP: {}", pass, index, idx, relTarget );
                        }
                    }
                    else
                    {
                        logger.info( "{}.{}.{}. SKIP (already discovered): {}", pass, index, idx, relTarget );
                    }

                    idx++;
                }

                // if all relationships have been discarded by filter...
                if ( !contributedRels && !discoveredRels.isEmpty() )
                {
                    logger.info( "{}.{}. INJECT: Adding terminal parent relationship to mark {} as resolved in the dependency graph.", pass, index,
                                 result.getSelectedRef() );

                    toStore.add( new ParentRelationship( config.getDiscoverySource(), result.getSelectedRef() ) );
                }
            }
            else
            {
                logger.info( "{}.{}. discovered relationships were NULL for: {}", pass, r.getIndex(), result.getSelectedRef() );
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Accumulate the next-step filters for any filters that accept the given relationship.
     * 
     * @param rel The relationship to apply current filters to
     * @param filters The filters to apply
     * @param pass For diagnostics/logging, this is the discovery-batch number 
     * since discovery was invoked
     * @param index For diagnostics/logging, this is the index of the discovery 
     * runnable/todo that led to discovery of the given relationship 
     * @param idx For diagnostics/logging, this is the index of the relationship 
     * we're currently processing for next-step todo's
     * @return The filters to be applied to the next crop of todo's resulting 
     * from the current relationship.
     */
    private Set<ProjectRelationshipFilter> findNextFilters( final ProjectRelationship<?> rel, final Set<ProjectRelationshipFilter> filters,
                                                            final int pass, final int index, final int idx )
    {
        final Set<ProjectRelationshipFilter> nextFilters = new HashSet<ProjectRelationshipFilter>();

        int fidx = 0;
        for ( final ProjectRelationshipFilter filter : filters )
        {
            final boolean accepted = filter.accept( rel );
            logger.info( "{}.{}.{}.{}. CHECK: {}\n  vs.\n\n  {}\n\n  Accepted? {}", pass, index, idx, fidx, rel, filter, accepted );
            if ( accepted )
            {
                nextFilters.add( filter.getChildFilter( rel ) );
            }
            fidx++;
        }

        return nextFilters;
    }

    /**
     * Accumulate new filters and mutators into existing {@link DiscoveryTodo} 
     * instances for the next round of discovery. If there is no existing todo 
     * for the given GAV, create one and add it.
     * 
     * @param ref The GAV to discover
     * @param nextFilters The list of filters to add to the discovery step
     * @param nextMutator The mutator to add to the discovery step (may be null)
     * @param nextTodos The current mapping of next-step todo's, which accumulates 
     * via successive calls to this method.
     */
    private void incorporateTodoInfo( final ProjectVersionRef ref, final Set<ProjectRelationshipFilter> nextFilters,
                                      final Map<ProjectVersionRef, DiscoveryTodo> nextTodos, final Set<GraphMutator> nextMutators )
    {
        DiscoveryTodo todo = nextTodos.get( ref );
        if ( todo == null )
        {
            Set<GraphMutator> childMutators = new HashSet<GraphMutator>();
            if ( nextMutators != null && !nextMutators.isEmpty() )
            {
                childMutators = new HashSet<GraphMutator>( nextMutators );
            }

            todo = new DiscoveryTodo( ref, nextFilters, childMutators );
            nextTodos.put( ref, todo );
        }
        else
        {
            todo.getFilters()
                .addAll( nextFilters );

            if ( nextMutators != null && !nextMutators.isEmpty() )
            {
                Set<GraphMutator> childMutators = todo.getMutators();
                if ( childMutators == null )
                {
                    childMutators = new HashSet<GraphMutator>();
                    todo.setMutators( childMutators );
                }

                childMutators.addAll( nextMutators );
            }
        }
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

    private void markMissing( final DiscoveryRunnable runnable, final Set<ProjectVersionRef> missing, final int pass )
    {
        final int index = runnable.getIndex();

        final ProjectVersionRef originalRef = runnable.getTodo()
                                                      .getRef();

        logger.info( "{}.{}. MISSING(1) += {}", pass, index, originalRef );
        missing.add( originalRef );

        final DiscoveryResult result = runnable.getResult();
        if ( result != null )
        {
            final ProjectVersionRef selectdRef = runnable.getResult()
                                                         .getSelectedRef();

            if ( !originalRef.equals( selectdRef ) )
            {
                logger.info( "{}.{}. MISSING(2) += {}", pass, index, selectdRef );
                missing.add( selectdRef );
            }
        }
    }

    private Set<ProjectVersionRef> loadExistingCycleParticipants( final EProjectNet net )
    {
        final Set<ProjectVersionRef> participants = new HashSet<ProjectVersionRef>();
        final Set<EProjectCycle> cycles = net.getCycles();
        for ( final EProjectCycle cycle : cycles )
        {
            participants.addAll( cycle.getAllParticipatingProjects() );
        }

        return participants;
    }

    private LinkedList<DiscoveryTodo> loadInitialPending( final EProjectNet net, final GraphMutator rootMutator )
    {
        final GraphView view = net.getView();
        final ProjectRelationshipFilter topFilter = view.getFilter();

        final GraphMutator topMutator = rootMutator == null ? new ManagedDependencyMutator( view, true ) : rootMutator;
        logger.info( "Using root-level mutator: {}", topMutator );

        final Set<ProjectVersionRef> initialIncomplete = net.getIncompleteSubgraphs();
        if ( initialIncomplete == null || initialIncomplete.isEmpty() )
        {
            return new LinkedList<DiscoveryTodo>();
        }

        final Set<List<ProjectRelationship<?>>> paths = net.getPathsTo( initialIncomplete.toArray( new ProjectVersionRef[initialIncomplete.size()] ) );

        if ( paths == null || paths.isEmpty() )
        {
            return new LinkedList<DiscoveryTodo>();
        }

        logger.info( "Finding paths from: {} to:\n  {}", join( net.getView()
                                                                  .getRoots(), ", " ), join( initialIncomplete, "\n  " ) );

        final Map<ProjectVersionRef, Set<ProjectRelationshipFilter>> filtersByRef = new HashMap<ProjectVersionRef, Set<ProjectRelationshipFilter>>();
        final Map<ProjectVersionRef, Set<GraphMutator>> mutatorsByRef = new HashMap<ProjectVersionRef, Set<GraphMutator>>();
        calculateInitialFiltersAndMutators( paths, topFilter, topMutator, filtersByRef, mutatorsByRef );

        final LinkedList<DiscoveryTodo> initialPending = new LinkedList<DiscoveryTodo>();
        for ( final Entry<ProjectVersionRef, Set<ProjectRelationshipFilter>> entry : filtersByRef.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final Set<ProjectRelationshipFilter> pathFilters = entry.getValue();
            final Set<GraphMutator> pathMutators = mutatorsByRef.get( ref );

            if ( pathFilters.isEmpty() )
            {
                logger.info( "INIT-SKIP: {}", ref );
                continue;
            }

            final DiscoveryTodo todo = new DiscoveryTodo( ref );
            todo.setFilters( pathFilters );
            todo.setMutators( pathMutators );

            logger.info( "INIT-DISCOVER += {}\n====\nfilters:\n    {}\n---\nmutators:\n  {}\n)", ref, new Object()
            {
                @Override
                public String toString()
                {
                    return join( pathFilters, "\n    " );
                }
            }, new Object()
            {
                @Override
                public String toString()
                {
                    return join( pathMutators, "\n    " );
                }
            } );

            initialPending.add( todo );
        }

        logger.info( "{} initial pending subgraphs:\n\n  {}\n", initialPending.size(), join( initialPending, "\n  " ) );

        return initialPending;
    }

    private void calculateInitialFiltersAndMutators( final Set<List<ProjectRelationship<?>>> paths, final ProjectRelationshipFilter topFilter,
                                                     final GraphMutator topMutator,
                                                     final Map<ProjectVersionRef, Set<ProjectRelationshipFilter>> filtersByRef,
                                                     final Map<ProjectVersionRef, Set<GraphMutator>> mutatorsByRef )
    {
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
                pathFilters = new HashSet<ProjectRelationshipFilter>();
                filtersByRef.put( ref, pathFilters );
            }

            Set<GraphMutator> pathMutators = mutatorsByRef.get( ref );
            if ( pathMutators == null )
            {
                pathMutators = new HashSet<GraphMutator>();
                mutatorsByRef.put( ref, pathMutators );
            }

            ProjectRelationshipFilter f = topFilter;
            GraphMutator m = topMutator;
            for ( final ProjectRelationship<?> rel : path )
            {
                if ( !f.accept( rel ) )
                {
                    continue nextPath;
                }

                logger.info( "Computing filtering/mutator for path step: {}", rel );
                m = m.getMutatorFor( rel );
                f = f.getChildFilter( rel );
            }

            logger.debug( "Adding todo: {} via filter: {}", ref, f );
            pathFilters.add( f );
            pathMutators.add( m );
        }
    }

}
