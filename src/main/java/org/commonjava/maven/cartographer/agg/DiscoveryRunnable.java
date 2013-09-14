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

    public DiscoveryRunnable( final DiscoveryTodo todo, final AggregationOptions config, final Set<ProjectVersionRef> missing,
                              final ProjectRelationshipDiscoverer discoverer, final boolean storeRelationships )
    {
        this.todo = todo;
        this.config = config;
        this.roMissing = missing;
        this.discoverer = discoverer;
        this.storeRelationships = storeRelationships;
    }

    @Override
    public void run()
    {
        final ProjectVersionRef ref = todo.getRef();

        logger.info( "\n\n\n\nProcessing missing project: %s\n\n\n\n", ref );

        try
        {
            final DiscoveryConfig discoveryConfig = config.getDiscoveryConfig();

            if ( discoverer != null && !roMissing.contains( ref ) )
            {
                result = discoverer.discoverRelationships( ref, discoveryConfig, storeRelationships );
            }
            else if ( roMissing.contains( ref ) )
            {
                logger.info( "MISS: Already marked as missing: %s", ref );
            }
            else
            {
                logger.info( "No discoverer! Skipping: %s", ref );
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
            if ( latch != null )
            {
                latch.countDown();
            }
        }
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
