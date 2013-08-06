package org.commonjava.maven.cartographer.event;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.util.logging.Logger;

public abstract class AbstractCartoEventManager
    implements CartoEventManager
{

    protected final Logger logger = new Logger( getClass() );

    private final Map<ProjectVersionRef, LockState> locks = new HashMap<ProjectVersionRef, LockState>();

    private CartoDataManager dataManager;

    protected AbstractCartoEventManager()
    {
    }

    protected AbstractCartoEventManager( final CartoDataManager dataManager )
    {
        this.dataManager = dataManager;
    }

    @Override
    public void unlockOnNewRelationshipsEvent( final NewRelationshipsEvent evt )
    {
        logger.info( "Unlocking %s due to new relationships available.", evt.getRef() );
        notifyOfGraph( evt.getRef() );
    }

    @Override
    public void unlockOnRelationshipsErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
        final ErrorKey key = evt.getKey();
        logger.info( "Unlocking %s due to error in model.", key );
        try
        {
            final ProjectVersionRef ref = key.toProjectVersionRef();
            notifyOfGraph( ref );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot parse version for error key: '%s'. Failed to unlock waiting threads. Reason: %s", e,
                          key.toString(), e.getMessage() );
        }
    }

    /*
     * In case we get here before the thread calling waitFor(), we'll inject a new
     * lock, unlock it, and wait a little while for the waitFor() method to run,
     * before removing it and exiting.
     */
    protected void notifyOfGraph( final ProjectVersionRef ref )
    {
        logger.info( "\n\nLooking up lock for: %s\n\n", ref );
        LockState lock;
        synchronized ( ref )
        {
            lock = locks.get( ref );
            if ( lock == null )
            {
                logger.info( "No lock found. Creating an unlocked one for: %s", ref );
                lock = new LockState();
                lock.unlock();
                locks.put( ref, lock );
            }
            else
            {
                synchronized ( lock )
                {
                    logger.info( "Found lock. Unlocking and notifying all watchers." );
                    lock.unlock();
                    lock.notifyAll();
                }
            }
        }

        final LockState lck = lock;
        final Thread t = new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                logger.info( "Starting notification timer on: %s for lagging callers.", ref );
                for ( int i = 0; i < 10; i++ )
                {
                    if ( !locks.containsKey( ref ) )
                    {
                        break;
                    }

                    synchronized ( lck )
                    {
                        try
                        {
                            lck.wait( 500 );
                            lck.notifyAll();
                        }
                        catch ( final InterruptedException e )
                        {
                            break;
                        }
                    }
                }

                logger.info( "Removing lock: %s", ref );
                locks.remove( ref );
            }
        } );

        t.setDaemon( true );
        t.setPriority( Thread.MIN_PRIORITY );
        t.start();
    }

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final long timeoutMillis )
        throws CartoDataException
    {
        if ( ref == null )
        {
            return;
        }

        if ( dataManager.getProjectGraph( ref ) != null )
        {
            return;
        }

        LockState lock = null;
        synchronized ( ref )
        {
            lock = locks.get( ref );
            if ( lock == null )
            {
                logger.info( "Submitting lock for: %s", ref );
                lock = new LockState();
                locks.put( ref, lock );
            }
        }

        long remaining = timeoutMillis;
        final long poll = 500;
        while ( remaining > 0 )
        {
            if ( !lock.isLocked() || dataManager.getProjectGraph( ref ) != null )
            {
                synchronized ( lock )
                {
                    lock.notifyAll();
                }
                locks.remove( ref );
                return;
            }

            final long toWait = poll > remaining ? remaining : poll;
            //            logger.info( "\n\n\n[POLL] GAV: %s\nMy thread priority is: %d\nWaiting %d ms.\n\nTotal time remaining: %d ms\n\n\n",
            //                         ref, Thread.currentThread()
            //                                    .getPriority(), toWait, remaining );

            synchronized ( lock )
            {
                try
                {
                    lock.wait( toWait );
                }
                catch ( final InterruptedException e )
                {
                    return;
                }
            }

            remaining -= toWait;
        }
    }

    private static final class LockState
    {
        private boolean locked = true;

        void unlock()
        {
            locked = false;
        }

        boolean isLocked()
        {
            return locked;
        }
    }
}
