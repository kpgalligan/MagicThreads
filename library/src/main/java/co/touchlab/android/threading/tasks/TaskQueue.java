package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.SubscriberExceptionEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Relatively simple queue implementation.  Supports removing tasks by type.  Simple method of
 * preventing odd results when old messages are put on the queue.
 * <p/>
 * Created by kgalligan on 7/5/14.
 */
public class TaskQueue
{
    private static Map<String, TaskQueueActual> queueMap = new HashMap<String, TaskQueueActual>();
    private static final String DEFAULT_QUEUE = "__DEFAULT";

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

    private static synchronized TaskQueueActual loadQueue(String name)
    {
        TaskQueueActual taskQueueActual = queueMap.get(name);
        if(taskQueueActual == null)
        {
            taskQueueActual = new TaskQueueActual();
            queueMap.put(name, taskQueueActual);
        }

        return taskQueueActual;
    }

    /**
     * Puts a task on the queue.
     *
     * @param context
     * @param task
     */
    public static void execute(Context context, Task task)
    {
        execute(context, DEFAULT_QUEUE, task);
    }

    /**
     * Puts a task on the queue. Use for local ops. Faster responses.
     *
     * @param context
     * @param task
     */
    public static void execute(Context context, String queue, Task task)
    {
        TaskQueueActual queueActual = loadQueue(queue);
        queueActual.execute(context, task);
    }
}
