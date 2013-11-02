package org.commonjava.maven.cartographer.agg;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.util.logging.Logger;

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

    public DiscoveryRunnable( final DiscoveryTodo todo, final AggregationOptions config, final Set<ProjectVersionRef> missing,
                              final ProjectRelationshipDiscoverer discoverer, final boolean storeRelationships, final int pass, final int idx )
    {
        this.todo = todo;
        this.config = config;
        this.roMissing = missing;
        this.discoverer = discoverer;
        this.storeRelationships = storeRelationships;
        this.pass = pass;
        this.idx = idx;
    }

    @Override
    public void run()
    {
        if ( Thread.currentThread()
                   .isInterrupted() )
        {
            return;
        }

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

}
