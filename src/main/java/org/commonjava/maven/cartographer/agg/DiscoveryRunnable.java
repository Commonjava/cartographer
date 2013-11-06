package org.commonjava.maven.cartographer.agg;

import static org.apache.commons.lang.StringUtils.join;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.util.logging.Logger;
import org.commonjava.util.logging.helper.JoinString;

public class DiscoveryRunnable
    implements Runnable
{

    private final AggregationOptions config;

    private CountDownLatch latch;

    private final Logger logger = new Logger( getClass() );

    private final Set<ProjectVersionRef> roMissing;

    private final ProjectRelationshipDiscoverer discoverer;

    private final DiscoveryTodo todo;

    private DiscoveryResult result;

    private final boolean storeRelationships;

    private final int pass;

    private final int idx;

    private final Map<ProjectVersionRef, Set<ProjectRelationshipFilter>> newTodos = new HashMap<>();

    private final Set<ProjectVersionRef> allProjects;

    public DiscoveryRunnable( final DiscoveryTodo todo, final AggregationOptions config, final Set<ProjectVersionRef> allProjects,
                              final Set<ProjectVersionRef> missing, final ProjectRelationshipDiscoverer discoverer, final boolean storeRelationships,
                              final int pass, final int idx )
    {
        this.todo = todo;
        this.config = config;
        this.allProjects = allProjects;
        this.roMissing = missing;
        this.discoverer = discoverer;
        this.storeRelationships = storeRelationships;
        this.pass = pass;
        this.idx = idx;
    }

    @Override
    public void run()
    {
        final ProjectVersionRef ref = todo.getRef();

        logger.info( "\n\n\n\n%d.%d. Processing missing project: %s\n\n\n\n", pass, idx, ref );

        try
        {
            final DiscoveryConfig discoveryConfig = config.getDiscoveryConfig();

            if ( discoverer != null && !roMissing.contains( ref ) )
            {
                result = discoverer.discoverRelationships( ref, discoveryConfig, storeRelationships );
            }
            else if ( roMissing.contains( ref ) )
            {
                logger.info( "%d.%d. MISS: Already marked as missing: %s", pass, idx, ref );
            }
            else
            {
                logger.info( "No discoverer! Skipping: %s", ref );
            }

            final Set<ProjectRelationship<?>> discoveredRels = result.getAcceptedRelationships();
            if ( discoveredRels != null )
            {
                logger.info( "%d.%d. Processing %d new relationships for: %s\n\n  %s", pass, idx, discoveredRels.size(), result.getSelectedRef(),
                             join( discoveredRels, "\n  " ) );

                int index = 0;
                final Set<ProjectRelationship<?>> reject = new HashSet<>();
                for ( final ProjectRelationship<?> rel : discoveredRels )
                {
                    final ProjectVersionRef relTarget = rel.getTarget()
                                                           .asProjectVersionRef();
                    if ( !allProjects.contains( relTarget ) )
                    {
                        final Set<ProjectRelationshipFilter> acceptingChildren = new HashSet<>();
                        int fidx = 0;
                        for ( final ProjectRelationshipFilter filter : todo.getFilters() )
                        {
                            final boolean accepted = filter.accept( rel );
                            logger.info( "%d.%d.%d.%d. CHECK: %s\n  vs.\n\n  %s\n\n  Accepted? %s", pass, idx, index, fidx, rel, filter, accepted );
                            if ( accepted )
                            {
                                acceptingChildren.add( filter.getChildFilter( rel ) );
                            }
                            fidx++;
                        }

                        if ( !acceptingChildren.isEmpty() )
                        {
                            logger.info( "%d.%d.%d. DISCOVER += %s\n  (filters:\n    %s)", pass, index, idx, relTarget,
                                         new JoinString( "\n    ", acceptingChildren ) );

                            newTodos.put( relTarget, acceptingChildren );
                        }
                        else
                        {
                            logger.info( "%d.%d.%d. SKIP: %s", pass, index, idx, relTarget );
                            reject.add( rel );
                        }
                    }
                    else
                    {
                        logger.info( "%d.%d.%d. SKIP (already discovered): %s", pass, index, idx, relTarget );
                    }

                    index++;
                }

                if ( !reject.isEmpty() )
                {
                    result = new DiscoveryResult( result.getSource(), result, reject );
                }
            }
            else
            {
                logger.info( "discovered relationships NULL for: %s", result.getSelectedRef() );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "%d.%d. Cannot discover subgraph for: %s. Reason: %s.", e, pass, idx, ref, e.getMessage() );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "%d.%d. Failed to discover subgraph for: %s. Reason: %s.", e, pass, idx, ref, e.getMessage() );
        }
        finally
        {
            if ( latch != null )
            {
                latch.countDown();
            }
        }
    }

    public int getIndex()
    {
        return idx;
    }

    public DiscoveryResult getResult()
    {
        return result;
    }

    public DiscoveryTodo getTodo()
    {
        return todo;
    }

    public void setLatch( final CountDownLatch latch )
    {
        this.latch = latch;
    }

    public Map<ProjectVersionRef, Set<ProjectRelationshipFilter>> getNewTodos()
    {
        return newTodos;
    }

}
