package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import co.touchlab.android.threading.utils.UiThreadContext;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.SubscriberExceptionEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Relatively simple queue implementation.  See TaskQueueActual for detail on implementation.
 * <p/>
 * Created by kgalligan on 7/5/14.
 */
public class TaskQueue
{
    private static Map<String, TaskQueueActual> queueMap = new HashMap<String, TaskQueueActual>();
    private static final String DEFAULT_QUEUE = "__DEFAULT";



    /**
     * Get a direct reference to your queue.  Call on main thread.
     * @param name
     * @return
     */
    public static synchronized TaskQueueActual loadQueue(String name)
    {
        TaskQueueActual taskQueueActual = queueMap.get(name);
        if(taskQueueActual == null)
        {
            taskQueueActual = new TaskQueueActual();
            queueMap.put(name, taskQueueActual);
        }

        return taskQueueActual;
    }

    public static TaskQueueActual loadQueueDefault()
    {
        return loadQueue(DEFAULT_QUEUE);
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
