package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import co.touchlab.android.threading.tasks.TaskQueue.Task;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.SubscriberExceptionEvent;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by kgalligan on 7/28/14.
 */
public class TaskQueueActual
{
    private LinkedBlockingQueue<Task> tasks = new LinkedBlockingQueue<Task>();
    private QueueThread queueThread;
    private Task currentTask;
    private Application application;

    public TaskQueueActual()
    {
        queueThread = new QueueThread();
        queueThread.start();

        //Is this strictly necessary?  Not sure.  The whole mess will get shut down anyway, but probably best to kill threads explicitly.  TODO: look into this.
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                shutdown();
            }
        });
    }

    public static class ErrorListener
    {
        public void onEvent(SubscriberExceptionEvent exceptionEvent)
        {
            final Throwable throwable = exceptionEvent.throwable;

            //EventBus will just log this.  Would prefer to blow up.
            new Thread()
            {
                @Override
                public void run()
                {
                    if (throwable instanceof RuntimeException)
                        throw (RuntimeException) throwable;
                    else if (throwable instanceof Error)
                        throw (Error) throwable;
                    else
                        throw new RuntimeException(throwable);
                }
            }.start();
        }
    }


    private class QueueThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                while (true)
                {

                    Task task = tasks.take();

                    setCurrentTask(task);

                    try
                    {
                        task.run(application);

                        setCurrentTask(null);
                    }
                    catch (Exception e)
                    {
                        boolean handled = task.handleError(e);
                        if (!handled)
                            throw new RuntimeException(e);
                    }

                    if (Thread.interrupted())
                    {
                        throw new InterruptedException();
                    }
                }
            }
            catch (InterruptedException e)
            {
                //
            }
            finally
            {
                killThread();
            }
        }
    }

    private synchronized Task getCurrentTask()
    {
        return currentTask;
    }

    private synchronized void setCurrentTask(Task currentTask)
    {
        this.currentTask = currentTask;
    }

    private synchronized void shutdown()
    {
        if (queueThread != null)
            queueThread.interrupt();
    }

    /**
     * Puts a task on the queue.
     *
     * @param context
     * @param task
     */
    public synchronized void execute(Context context, Task task)
    {
        //repeatedly assigning seems ugly, but should work.
        application = (Application) context.getApplicationContext();
        tasks.add(task);
    }

    /**
     * Makes sure only one task of a type on the queue.  This would be useful on a search screen,
     * for example.  If the first search was processing, and the user clicked search again, the
     * first task shouldn't do anything to the screen when it returns.
     *
     * @param context
     * @param task
     */
    public synchronized void executeSingleByType(Context context, Task task)
    {
        removeTasksByType(task.getClass());
        execute(context, task);
    }

    private synchronized void killThread()
    {
        queueThread = null;
    }

    /**
     * Removes tasks of a type from the queue.  Useful if you're worried about old tasks returning unexpectedly,
     * or only want one task of a type running (more precisely, finishing).
     * <p/>
     * There is a potential here for issues.  We can't easily block the take portion of the loop,
     * so this may result in unexpected issues.
     *
     * @param c
     */
    public synchronized void removeTasksByType(Class c)
    {
        checkTasksQueue(c);
        checkCurrentTaskForRemoval(c);
    }

    private synchronized void checkTasksQueue(Class c)
    {
        Iterator<Task> taskIterator = tasks.iterator();
        while (taskIterator.hasNext())
        {
            Task next = taskIterator.next();
            if (c.equals(next.getClass()))
                taskIterator.remove();
        }
    }

    private synchronized void checkCurrentTaskForRemoval(Class c)
    {
        Task currentTask = getCurrentTask();
        if (currentTask.getClass().equals(c))
            setCurrentTask(null);
    }
}
