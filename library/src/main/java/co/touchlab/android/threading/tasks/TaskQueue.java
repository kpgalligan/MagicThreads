package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import android.os.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import co.touchlab.android.threading.utils.UiThreadContext;

/**
 * Relatively simple queue implementation.  See TaskQueueActual for detail on implementation.
 * <p/>
 * Created by kgalligan on 7/5/14.
 */
public class TaskQueue extends BaseTaskQueue
{
    private static Map<String, TaskQueue> queueMap = new HashMap<String, TaskQueue>();
    private static final String DEFAULT_QUEUE = "__DEFAULT";

    /**
     * Get a direct reference to your queue.  Call on main thread.
     *
     * @param name
     * @return
     */
    public static synchronized TaskQueue loadQueue(Context context, String name)
    {
        TaskQueue taskQueueActual = queueMap.get(name);
        if (taskQueueActual == null)
        {
            taskQueueActual = new TaskQueue((Application) context.getApplicationContext());
            queueMap.put(name, taskQueueActual);
        }

        return taskQueueActual;
    }

    /**
     * The default queue
     *
     * @return
     */
    public static TaskQueue loadQueueDefault(Context context)
    {
        return loadQueue(context, DEFAULT_QUEUE);
    }

    //*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~
    //*~*~*~*~*~*~*~*~*~*~ PER INSTANCE *~*~*~*~*~*~*~*~*~*~*~*~*~
    //*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~


    public TaskQueue(Application application)
    {
        super(application);
    }

    @Override
    protected Queue<Task> createQueue()
    {
        return new LinkedList<Task>();
    }

    @Override
    protected void runTask(Task task)
    {
        executorService.execute(new ExeTask(task));
    }

    @Override
    protected void finishTask(Message msg, Task task)
    {
        try
        {
            if (task != null)
            {
                task.onComplete(application);
            }
        }
        finally
        {
            resetPollRunnable();
        }
    }

    /**
     * Puts a task on the queue.  Call on main thread only.
     *
     * @param task
     */
    public void execute(final Task task)
    {
        if (UiThreadContext.isInUiThread())
        {
            insertTask(task);
        }
        else
        {
            Message message = handler.obtainMessage(QueueHandler.INSERT_TASK, task);
            handler.sendMessage(message);
        }
    }

    private class ExeTask implements Runnable
    {
        private Task task;

        private ExeTask(Task task)
        {
            this.task = task;
        }

        @Override
        public void run()
        {
            UiThreadContext.assertBackgroundThread();

            try
            {
                task.run(application);
            }
            catch (Throwable e)
            {
                boolean handled = task.handleError(application, e);
                if (!handled)
                {
                    handler.sendMessage(handler.obtainMessage(QueueHandler.THROW, e));
                }
            }
            finally
            {
                handler.sendMessage(handler.obtainMessage(QueueHandler.POST_EXE));
            }
        }
    }


}
