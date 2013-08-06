package org.commonjava.maven.cartographer.agg;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.util.logging.Logger;

public class DiscoveryRunnable
    implements Runnable
{

    //    private final LinkedList<EProjectNet> graphs;

    private final AggregationOptions config;

    private final CountDownLatch latch;

    private final Logger logger = new Logger( getClass() );

    private final Set<ProjectVersionRef> roMissing;

    private final Set<ProjectVersionRef> newMissing = new HashSet<>();

    private final Set<DiscoveryTodo> newTodos = new HashSet<>();

    private final Set<ProjectVersionRef> newCycleParticipants = new HashSet<>();

    //    private final Set<Builder> cycleBuilders;

    private final ProjectRelationshipDiscoverer discoverer;

    private final CartoDataManager data;

    private final DiscoveryTodo todo;

    public DiscoveryRunnable( final DiscoveryTodo todo,
                              //                              final LinkedList<EProjectNet> graphs, final Set<EProjectCycle.Builder> cycleBuilders,
                              final AggregationOptions config, final Set<ProjectVersionRef> missing,
                              final ProjectRelationshipDiscoverer discoverer, final CartoDataManager data,
                              final CountDownLatch latch )
    {
        this.todo = todo;
        //        this.graphs = graphs;
        //        this.cycleBuilders = cycleBuilders;
        this.config = config;
        this.roMissing = missing;
        this.discoverer = discoverer;
        this.data = data;
        this.latch = latch;
    }

    @Override
    public void run()
    {
        final ProjectVersionRef ref = todo.getRef();

        logger.info( "\n\n\n\nProcessing missing project: %s\n\n\n\n", ref );

        try
        {
            final DiscoveryConfig discoveryConfig = config.getDiscoveryConfig();

            //            final Set<ProjectRelationship<?>> rels = data.getAllDirectRelationshipsWithExactTarget( ref, filter );
            //            if ( detectCycles( ref, rels, graphs, cycleBuilders, filter, cycleParticipants ) )
            //            {
            //                return;
            //            }

            if ( discoverer != null && !roMissing.contains( ref ) )
            {
                // kick off the async process of resolving metadata and building the graph.
                // then, wait for the event funnel to capture something telling us to re-check
                // for the project graph.
                final DiscoveryResult result = discoverer.discoverRelationships( ref, discoveryConfig );
                final ProjectVersionRef newRef = result.getSelectedRef();

                addToCycleParticipants( result.getRejectedRelationships() );

                if ( newRef.isVariableVersion() || !data.contains( newRef ) )
                {
                    markMissing( newRef );
                }
                else
                {
                    final ProjectRelationshipFilter filter = todo.getFilter();

                    final Set<ProjectRelationship<?>> newRels = result.getAcceptedRelationships();

                    for ( final ProjectRelationship<?> rel : newRels )
                    {
                        if ( !data.contains( newRef ) && filter.accept( rel ) )
                        {
                            final ProjectRelationshipFilter childFilter = filter.getChildFilter( rel );
                            newTodos.add( new DiscoveryTodo( newRef, childFilter ) );
                        }
                    }
                }
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot discover subgraph for: %s. Reason: %s.", e, ref, e.getMessage() );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to discover subgraph for: %s. Reason: %s.", e, ref, e.getMessage() );
        }
        finally
        {
            latch.countDown();
        }
    }

    private void addToCycleParticipants( final Set<ProjectRelationship<?>> rejectedRelationships )
    {
        for ( final ProjectRelationship<?> rejected : rejectedRelationships )
        {
            newCycleParticipants.add( rejected.getDeclaring()
                                              .asProjectVersionRef() );
            newCycleParticipants.add( rejected.getTarget()
                                              .asProjectVersionRef() );
        }
    }

    private void markMissing( final ProjectVersionRef ref )
    {
        newMissing.add( ref );
        final ProjectVersionRef originalRef = todo.getRef();
        if ( !originalRef.equals( ref ) )
        {
            newMissing.add( originalRef );
        }
    }

    //    private boolean detectCycles( final ProjectVersionRef ref, final Set<ProjectRelationship<?>> rels,
    //                                  final List<EProjectNet> graphs, final Set<Builder> cycleBuilders,
    //                                  final ProjectRelationshipFilter filters,
    //                                  final Set<ProjectVersionRef> cycleParticipants )
    //    {
    //        boolean foundCycle = false;
    //
    //        for ( final EProjectCycle.Builder cycleBuilder : cycleBuilders )
    //        {
    //            final int idx = cycleBuilder.indexOf( ref );
    //            if ( idx > -1 )
    //            {
    //                // CYCLE!!!
    //                final EProjectCycle.Builder cb = new EProjectCycle.Builder( cycleBuilder, idx );
    //                for ( final ProjectRelationship<?> rel : rels )
    //                {
    //                    cycleParticipants.add( rel.getDeclaring() );
    //                    cycleParticipants.add( rel.getTarget()
    //                                              .asProjectVersionRef() );
    //
    //                    if ( !filters.accept( rel ) )
    //                    {
    //                        continue;
    //                    }
    //
    //                    cb.with( rel );
    //
    //                    final EProjectCycle cycle = cb.build();
    //                    //                    logger.info( "CYCLE! %s", cycle );
    //
    //                    for ( final EProjectNet g : graphs )
    //                    {
    //                        g.addCycle( cycle );
    //                    }
    //
    //                    cb.withoutLast();
    //                    foundCycle = true;
    //                }
    //            }
    //        }
    //
    //        return foundCycle;
    //    }

    public Set<DiscoveryTodo> getNewTodos()
    {
        return newTodos;
    }

    public Set<ProjectVersionRef> getNewMissing()
    {
        return newMissing;
    }

    public Set<ProjectVersionRef> getNewCycleParticipants()
    {
        return newCycleParticipants;
    }

}
