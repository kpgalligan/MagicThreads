package co.touchlab.android.threading.tasks;

import de.greenrobot.event.EventBus;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by kgalligan on 7/5/14.
 */
public class TaskQueue
{
    private static LinkedBlockingQueue<Task> tasks = new LinkedBlockingQueue<Task>();
    private static QueueThread queueThread;
    private static Task currentTask;

    static {
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
        void run() throws Exception;

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
                    Task task = topTask();

                    setCurrentTask(task);

                    try
                    {
                        task.run();
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

    public synchronized static Task getCurrentTask()
    {
        return currentTask;
    }

    public synchronized static void setCurrentTask(Task currentTask)
    {
        TaskQueue.currentTask = currentTask;
    }

    private static synchronized Task topTask() throws InterruptedException
    {
        return tasks.take();
    }

    private static synchronized void shutdown()
    {
        if(queueThread != null)
            queueThread.interrupt();
    }

    public static synchronized void execute(final Task task)
    {
        if(queueThread == null)
        {
            queueThread = new QueueThread();
            queueThread.start();
        }
        tasks.add(task);
    }

    public static synchronized void executeSingleByType(Task task)
    {
        removeTasksByType(task.getClass());
        execute(task);
    }

    private static synchronized void killThread()
    {
        queueThread = null;
    }

    public static synchronized void removeTasksByType(Class c)
    {
        Iterator<Task> taskIterator = tasks.iterator();
        while (taskIterator.hasNext())
        {
            Task next = taskIterator.next();
            if(c.equals(next.getClass()))
                taskIterator.remove();
        }

        Task currentTask = getCurrentTask();
        if(currentTask.getClass().equals(c))
            setCurrentTask(null);
    }
}
