package org.sonatype.nexus.scheduling.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.scheduling.ScheduleIterator;
import org.sonatype.nexus.scheduling.ScheduledCallableTask;
import org.sonatype.nexus.scheduling.SubmittedCallableTask;

public class DefaultCallableTask<T>
    extends AbstractSchedulerTask<T>
    implements SubmittedCallableTask<T>, ScheduledCallableTask<T>, Callable<T>
{
    private final Callable<T> callable;

    private List<T> results;

    public DefaultCallableTask( Callable<T> callable, ScheduleIterator scheduleIterator,
        ScheduledThreadPoolExecutor executor )
    {
        super( scheduleIterator, executor );

        this.callable = callable;

        this.results = new ArrayList<T>();
    }

    public void start()
    {
        setFuture( reschedule() );
    }

    // SubmittedCallableTask

    public T get()
        throws ExecutionException,
            InterruptedException
    {
        return getFuture().get();
    }

    public T getIfDone()
    {
        if ( isDone() )
        {
            try
            {
                return getFuture().get();
            }
            catch ( ExecutionException e )
            {
                return null;
            }
            catch ( InterruptedException e )
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    // ScheduledCallableTask

    public T getLast()
        throws ExecutionException,
            InterruptedException
    {
        return get();
    }

    public T getLastIfDone()
    {
        return getIfDone();
    }

    public int getResultCount()
    {
        return results.size();
    }

    public T get( int i )
        throws IndexOutOfBoundsException
    {
        return results.get( i );
    }

    // Other

    public T call()
        throws Exception
    {
        T result = null;

        if ( isEnabled() )
        {
            setLastRun( new Date() );

            result = callable.call();

            results.add( result );
        }

        Future<T> nextFuture = reschedule();

        if ( nextFuture != null )
        {
            setFuture( nextFuture );
        }

        return result;
    }

    protected Future<T> reschedule()
    {
        if ( getScheduleIterator() != null && !getScheduleIterator().isFinished() )
        {
            return getExecutor().schedule(
                this,
                getScheduleIterator().next().getTime() - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS );
        }
        else if ( getLastRun() == null )
        {
            return getExecutor().submit( this );
        }
        else
        {
            return null;
        }
    }

}
