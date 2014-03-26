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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DefaultGraphAggregator
    implements GraphAggregator
{

    //    private static final int MAX_BATCHSIZE = 50;

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
        if ( net != null && config.isDiscoveryEnabled() )
        {
            final GraphView view = net.getView();

            final Set<ProjectVersionRef> missing = new HashSet<ProjectVersionRef>();

            logger.debug( "Loading existing cycle participants..." );
            final Set<ProjectVersionRef> cycleParticipants = loadExistingCycleParticipants( net );

            final Set<ProjectVersionRef> seen = new HashSet<ProjectVersionRef>();

            logger.debug( "Loading initial set of GAVs to be resolved..." );
            final List<DiscoveryTodo> pending = loadInitialPending( view, seen, config.getMutator() );
            final HashSet<DiscoveryTodo> done = new HashSet<DiscoveryTodo>();

            int pass = 0;
            while ( !pending.isEmpty() )
            {
                final HashSet<DiscoveryTodo> current = new HashSet<DiscoveryTodo>( pending );
                pending.clear();

                //                final HashSet<DiscoveryTodo> current = new HashSet<DiscoveryTodo>( MAX_BATCHSIZE );
                //                while ( !pending.isEmpty() && current.size() < MAX_BATCHSIZE )
                //                {
                //                    current.add( pending.remove( 0 ) );
                //                }

                done.addAll( current );

                logger.debug( "{}. {} in next batch of TODOs:\n  {}", pass, current.size(), new JoinString( "\n  ", current ) );
                final Set<DiscoveryTodo> newTodos = discover( current, config, cycleParticipants, missing, seen, view, pass );
                if ( newTodos != null )
                {
                    logger.debug( "{}. Uncovered new batch of TODOs:\n  {}", pass, new JoinString( "\n  ", newTodos ) );

                    for ( final DiscoveryTodo todo : newTodos )
                    {
                        if ( !done.contains( todo ) && !pending.contains( todo ) )
                        {
                            logger.debug( "+= {}", todo );
                            pending.add( todo );
                        }
                    }
                }

                pass++;
            }

            logger.info( "Discovery complete. {} seen, {} missing in {} passes.", seen.size(), missing.size(), pass - 1 );
        }

        return net;
    }

    private Set<DiscoveryTodo> discover( final Set<DiscoveryTodo> todos, final AggregationOptions config,
                                         final Set<ProjectVersionRef> cycleParticipants, final Set<ProjectVersionRef> missing,
                                         final Set<ProjectVersionRef> seen, final GraphView view, final int pass )
        throws CartoDataException
    {
        logger.info( "Starting pass: {}", pass );
        logger.debug( "{}. Performing discovery and cycle-detection on {} missing subgraphs:\n  {}", pass, todos.size(), new JoinString( "\n  ",
                                                                                                                                         todos ) );

        final Set<DiscoveryRunnable> runnables = executeTodoBatch( todos, config, missing, seen, cycleParticipants, pass );

        logger.debug( "{}. Accounting for discovery results. Before discovery, these were missing:\n\n  {}\n\n", pass, new JoinString( "\n  ",
                                                                                                                                       missing ) );

        final Map<ProjectVersionRef, DiscoveryTodo> nextTodos = new HashMap<ProjectVersionRef, DiscoveryTodo>();

        for ( final DiscoveryRunnable r : runnables )
        {
            if ( !processDiscoveryOutput( r, nextTodos, view, config.getDiscoveryConfig(), seen, pass ) )
            {
                markMissing( r, missing, pass );
            }
        }

        logger.info( "{}. After discovery, {} are missing", pass, missing.size() );
        logger.debug( "Missing:\n\n  {}\n\n", new JoinString( "\n  ", missing ) );

        return new HashSet<DiscoveryTodo>( nextTodos.values() );
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
    private Set<DiscoveryRunnable> executeTodoBatch( final Set<DiscoveryTodo> todos, final AggregationOptions config,
                                                     final Set<ProjectVersionRef> missing, final Set<ProjectVersionRef> seen,
                                                     final Set<ProjectVersionRef> cycleParticipants, final int pass )
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
            // WAS: net.containsProject(todoRef) ...this is pretty expensive, since it requires traversal. Instead, we track as we go.
            else if ( seen.contains( todoRef ) )
            {
                logger.info( "{}.{}. Skipping already-discovered reference: {}", pass, idx++, todoRef );
                continue;
            }

            //            logger.info( "DISCOVER += {}", todo );
            final DiscoveryRunnable runnable = new DiscoveryRunnable( todo, config, roMissing, discoverer, pass, idx );
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
     * @param nextTodos The accumulated next crop of {@link DiscoveryTodo}'s, which 
     * MAY be augmented by output from this discovery runnable 
     * @param net The top-level dependency graph for which discovery is taking place
     * @param config Configuration for how discovery should proceed
     * @param seen 
     * @param pass For diagnostic/logging purposes, the number of discovery passes 
     * since discovery was initiated by the caller (part of the graph may have been pre-existing)
     * @return true if output contained a valid result, or false to indicate the 
     * GAV should be marked missing.
     * @throws CartoDataException 
     */
    private boolean processDiscoveryOutput( final DiscoveryRunnable r, final Map<ProjectVersionRef, DiscoveryTodo> nextTodos, final GraphView view,
                                            final DiscoveryConfig config, final Set<ProjectVersionRef> seen, final int pass )
        throws CartoDataException
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
                final Map<GraphPath<?>, GraphPathInfo> parentPathMap = todo.getParentPathMap();

                seen.add( todo.getRef() );

                final int index = r.getIndex();

                // De-selected relationships (not mutated) should be stored but NOT followed for discovery purposes.
                // Likewise, mutated (selected) relationships should be followed but NOT stored.
                logger.info( "{}.{}. Processing {} new relationships for: {}", pass, index, discoveredRels.size(), result.getSelectedRef() );
                logger.debug( "Relationships:\n  {}", new JoinString( "\n  ", discoveredRels ) );

                boolean contributedRels = false;

                int idx = 0;
                for ( final ProjectRelationship<?> rel : discoveredRels )
                {
                    final ProjectVersionRef relTarget = rel.getTarget()
                                                           .asProjectVersionRef();
                    if ( !seen.contains( relTarget ) )
                    {
                        for ( final Entry<GraphPath<?>, GraphPathInfo> entry : parentPathMap.entrySet() )
                        {
                            GraphPath<?> path = entry.getKey();
                            GraphPathInfo pathInfo = entry.getValue();

                            final ProjectRelationship<?> selected = pathInfo.selectRelationship( rel, path );
                            if ( selected == null )
                            {
                                continue;
                            }

                            final ProjectVersionRef selectedTarget = selected.getTarget()
                                                                             .asProjectVersionRef();
                            if ( seen.contains( selectedTarget ) )
                            {
                                continue;
                            }

                            contributedRels = true;

                            path = dataManager.graphs()
                                              .createPath( view, path, selected );

                            pathInfo = pathInfo.getChildPathInfo( selected );

                            DiscoveryTodo nextTodo = nextTodos.get( selectedTarget );
                            if ( nextTodo == null )
                            {
                                nextTodo = new DiscoveryTodo( selectedTarget, path, pathInfo );
                                nextTodos.put( selectedTarget, nextTodo );

                                logger.info( "DISCOVER += {}", selectedTarget );
                            }
                            else
                            {
                                nextTodo.addParentPath( path, pathInfo );
                            }
                        }

                        if ( rel.isManaged() )
                        {
                            logger.debug( "{}.{}.{}. FORCE; NON-TRAVERSE: Adding managed relationship (for mutator use later): {}", pass, index, idx,
                                          rel );
                            contributedRels = true;
                        }
                        else if ( rel.getType() == RelationshipType.PARENT )
                        {
                            logger.debug( "{}.{}.{}. FORCE; NON-TRAVERSE: Adding parent relationship: {}", pass, index, idx, rel );
                            contributedRels = true;
                        }
                        else
                        {
                            logger.debug( "{}.{}.{}. SKIP: {}", pass, index, idx, relTarget );
                        }
                    }
                    else
                    {
                        logger.debug( "{}.{}.{}. SKIP (already discovered): {}", pass, index, idx, relTarget );
                    }

                    idx++;
                }

                // if all relationships have been discarded by filter...
                if ( !contributedRels && !discoveredRels.isEmpty() )
                {
                    logger.debug( "{}.{}. INJECT: Adding terminal parent relationship to mark {} as resolved in the dependency graph.", pass, index,
                                  result.getSelectedRef() );

                    dataManager.storeRelationships( new ParentRelationship( config.getDiscoverySource(), result.getSelectedRef() ) );
                }
            }
            else
            {
                logger.debug( "{}.{}. discovered relationships were NULL for: {}", pass, r.getIndex(), result.getSelectedRef() );
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    //    private void addToCycleParticipants( final Set<ProjectRelationship<?>> rejectedRelationships, final Set<ProjectVersionRef> cycleParticipants )
    //    {
    //        for ( final ProjectRelationship<?> rejected : rejectedRelationships )
    //        {
    //            cycleParticipants.add( rejected.getDeclaring()
    //                                           .asProjectVersionRef() );
    //            cycleParticipants.add( rejected.getTarget()
    //                                           .asProjectVersionRef() );
    //        }
    //    }

    private void markMissing( final DiscoveryRunnable runnable, final Set<ProjectVersionRef> missing, final int pass )
    {
        final int index = runnable.getIndex();

        final ProjectVersionRef originalRef = runnable.getTodo()
                                                      .getRef();

        logger.debug( "{}.{}. MISSING(1) += {}", pass, index, originalRef );
        missing.add( originalRef );

        final DiscoveryResult result = runnable.getResult();
        if ( result != null )
        {
            final ProjectVersionRef selectdRef = result.getSelectedRef();

            if ( !originalRef.equals( selectdRef ) )
            {
                logger.debug( "{}.{}. MISSING(2) += {}", pass, index, selectdRef );
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

    private List<DiscoveryTodo> loadInitialPending( final GraphView view, final Set<ProjectVersionRef> seen, final GraphMutator rootMutator )
    {
        logger.info( "Using root-level mutator: {}", view.getMutator() );

        final Set<ProjectVersionRef> initialIncomplete = dataManager.graphs()
                                                                    .getAllIncompleteSubgraphs( view );

        if ( initialIncomplete == null || initialIncomplete.isEmpty() )
        {
            return new ArrayList<DiscoveryTodo>();
        }

        final Map<GraphPath<?>, GraphPathInfo> pathMap = dataManager.graphs()
                                                                    .getPathMapTargeting( view, initialIncomplete );

        if ( pathMap == null || pathMap.isEmpty() )
        {
            return new ArrayList<DiscoveryTodo>();
        }

        logger.info( "Finding paths to:\n  {} \n\nfrom:\n  {}\n\n", new JoinString( "\n ", initialIncomplete ),
                     new JoinString( "\n  ", view.getRoots() ) );

        final Map<ProjectVersionRef, DiscoveryTodo> initialPending = new HashMap<ProjectVersionRef, DiscoveryTodo>();

        for ( final Entry<GraphPath<?>, GraphPathInfo> entry : pathMap.entrySet() )
        {
            final GraphPath<?> path = entry.getKey();
            final GraphPathInfo pathInfo = entry.getValue();

            final ProjectVersionRef ref = dataManager.graphs()
                                                     .getPathTargetRef( view, path );

            DiscoveryTodo todo = initialPending.get( ref );
            if ( todo == null )
            {
                todo = new DiscoveryTodo( ref, path, pathInfo );
                initialPending.put( ref, todo );

                logger.info( "INIT-DISCOVER += {}", ref );
            }
            else
            {
                todo.addParentPath( path, pathInfo );
            }
        }

        logger.info( "[INIT] {} subgraphs pending discovery", initialPending.size() );
        logger.debug( "Initial pending:\n  {}\n", new JoinString( "\n  ", initialPending.keySet() ) );

        return new ArrayList<DiscoveryTodo>( initialPending.values() );
    }
}
