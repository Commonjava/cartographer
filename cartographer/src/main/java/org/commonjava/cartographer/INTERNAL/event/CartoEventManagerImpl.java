/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.INTERNAL.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.spi.event.CartoEventManager;
import org.commonjava.cartographer.spi.event.ProjectRelationshipsErrorEvent;
import org.commonjava.cartographer.spi.event.RelationshipStorageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named( "default-carto-event-mgr" )
public class CartoEventManagerImpl
        implements CartoEventManager
{

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<ProjectVersionRef, LockState> locks = new WeakHashMap<ProjectVersionRef, LockState>();

    @Override
    public void fireStorageEvent( final RelationshipStorageEvent evt )
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        final Collection<? extends ProjectRelationship<?, ?>> stored = evt.getStored();
        if ( stored != null )
        {
            for ( final ProjectRelationship<?, ?> rel : stored )
            {
                refs.add( rel.getDeclaring().asProjectVersionRef() );
            }
        }

        final Collection<ProjectRelationship<?, ?>> rejected = evt.getRejected();
        if ( rejected != null )
        {
            for ( final ProjectRelationship<?, ?> rel : rejected )
            {
                refs.add( rel.getDeclaring().asProjectVersionRef() );
            }
        }

        for ( final ProjectVersionRef ref : refs )
        {
            logger.debug( "Unlocking {} due to new relationships available.", ref );
            notifyOfGraph( ref );
        }
    }

    @Override
    public void fireErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
        final ProjectVersionRef ref = evt.getRef();
        logger.debug( "Unlocking {} due to error in model.", ref );
        try
        {
            notifyOfGraph( ref );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error(
                    String.format( "Cannot parse version for: '%s'. Failed to unlock waiting threads. Reason: %s", ref,
                                   e.getMessage() ), e );
        }
    }

    /*
     * In case we get here before the thread calling waitFor(), we'll inject a new
     * lock, unlock it, and wait a little while for the waitFor() method to run,
     * before removing it and exiting.
     */
    @Override
    public void notifyOfGraph( final ProjectVersionRef ref )
    {
        //        logger.info( "\n\nLooking up lock for: {}\n\n", ref );
        LockState lock;
        synchronized ( ref )
        {
            lock = locks.get( ref );
            if ( lock == null )
            {
                //                logger.info( "No lock found. Creating an unlocked one for: {}", ref );
                lock = new LockState();
                locks.put( ref, lock );
            }
        }

        lock.unlock();

        // NOTE: IF we have to reinstate this, use a different mech. that won't clog the system with untamed threads...
        //        final LockState lck = lock;
        //        final Thread t = new Thread( new Runnable()
        //        {
        //            @Override
        //            public void run()
        //            {
        //                //                logger.info( "Starting notification timer on: {} for lagging callers.", ref );
        //                for ( int i = 0; i < 10; i++ )
        //                {
        //                    if ( !locks.containsKey( ref ) )
        //                    {
        //                        break;
        //                    }
        //
        //                    synchronized ( lck )
        //                    {
        //                        try
        //                        {
        //                            lck.wait( 500 );
        //                        }
        //                        catch ( final InterruptedException e )
        //                        {
        //                            Thread.currentThread()
        //                                  .interrupt();
        //                            break;
        //                        }
        //                    }
        //                }
        //
        //                //                logger.info( "Removing lock: {}", ref );
        //                lck.unlock();
        //                locks.remove( ref );
        //            }
        //        } );
        //
        //        t.setDaemon( true );
        //        t.setPriority( Thread.MIN_PRIORITY );
        //        t.start();
    }

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final RelationshipGraph graph, final long timeoutMillis )
            throws CartoDataException
    {
        if ( ref == null )
        {
            return;
        }

        if ( graph.containsGraph( ref ) )
        {
            return;
        }

        LockState lock = null;
        synchronized ( ref )
        {
            lock = locks.get( ref );
            if ( lock == null )
            {
                logger.debug( "Submitting lock for: {}", ref );
                lock = new LockState();
                locks.put( ref, lock );
            }
        }

        long remaining = timeoutMillis;
        final long poll = 500;
        while ( remaining > 0 )
        {
            if ( !lock.isLocked() || graph.containsGraph( ref ) )
            {
                synchronized ( lock )
                {
                    lock.notifyAll();
                }
                locks.remove( ref );
                return;
            }

            final long toWait = poll > remaining ? remaining : poll;
            //            logger.info( "\n\n\n[POLL] GAV: {}\nMy thread priority is: {}\nWaiting {} ms.\n\nTotal time remaining: {} ms\n\n\n",
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
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            remaining -= toWait;
        }
    }

    private static final class LockState
    {
        private boolean locked = true;

        synchronized void unlock()
        {
            locked = false;
            notifyAll();
        }

        boolean isLocked()
        {
            return locked;
        }
    }

}
