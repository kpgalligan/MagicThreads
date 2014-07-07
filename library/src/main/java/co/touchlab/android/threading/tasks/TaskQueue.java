package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import de.greenrobot.event.EventBus;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Relatively simple queue implementation.  Supports removing tasks by type.  Simple method of
 * preventing odd results when old messages are put on the queue.
 *
 * Created by kgalligan on 7/5/14.
 */
public class TaskQueue
{
    private static LinkedBlockingQueue<Task> tasks = new LinkedBlockingQueue<Task>();
    private static QueueThread queueThread;
    private static Task currentTask;
    private static Application application;

    static {
        queueThread = new QueueThread();
        queueThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run()
            {
                shutdown();
            }
        });
    }

    public interface Task
    {
        void run(Context context) throws Exception;

        /**
         * Handle Exception that occurred during processing.  Return true if handled, false if not.  If not handled, app will throw and probably crash.
         *
         * @param e
         * @return true if handled, false if not (which will throw it)
         */
        boolean handleError(Exception e);
    }

    private static class QueueThread extends Thread
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
                        Task postTask = getCurrentTask();

                        //May be null if cleared out
                        if(postTask != null)
                            EventBus.getDefault().post(postTask);

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

    private synchronized static Task getCurrentTask()
    {
        return currentTask;
    }

    private synchronized static void setCurrentTask(Task currentTask)
    {
        TaskQueue.currentTask = currentTask;
    }

    private static synchronized void shutdown()
    {
        if(queueThread != null)
            queueThread.interrupt();
    }

    /**
     * Puts a task on the queue.
     *
     * @param context
     * @param task
     */
    public static synchronized void execute(Context context, Task task)
    {
        //repeatedly assigning seems ugly, but should work.
        application = (Application)context.getApplicationContext();
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
    public static synchronized void executeSingleByType(Context context, Task task)
    {
        removeTasksByType(task.getClass());
        execute(context, task);
    }

    private static synchronized void killThread()
    {
        queueThread = null;
    }

    /**
     * Removes tasks of a type from the queue.  Useful if you're worried about old tasks returning unexpectedly,
     * or only want one task of a type running (more precisely, finishing).
     *
     * There is a potential here for issues.  We can't easily block the take portion of the loop,
     * so this may result in unexpected issues.
     * @param c
     */
    public static void removeTasksByType(Class c)
    {
        checkTasksQueue(c);
        checkCurrentTaskForRemoval(c);
    }

    private static synchronized void checkTasksQueue(Class c)
    {
        Iterator<Task> taskIterator = tasks.iterator();
        while (taskIterator.hasNext())
        {
            Task next = taskIterator.next();
            if(c.equals(next.getClass()))
                taskIterator.remove();
        }
    }

    private static synchronized void checkCurrentTaskForRemoval(Class c)
    {
        Task currentTask = getCurrentTask();
        if(currentTask.getClass().equals(c))
            setCurrentTask(null);
    }
}
