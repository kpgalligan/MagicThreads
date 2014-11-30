package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import co.touchlab.android.threading.utils.UiThreadContext;
import javafx.embed.swt.SWTFXUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
     * @param name
     * @return
     */
    public static synchronized TaskQueue loadQueue(Context context, String name)
    {
        TaskQueue taskQueueActual = queueMap.get(name);
        if(taskQueueActual == null)
        {
            taskQueueActual = new TaskQueue((Application)context.getApplicationContext());
            queueMap.put(name, taskQueueActual);
        }

        return taskQueueActual;
    }

    /**
     * The default queue
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
    protected void runTask(Task task)
    {
        executorService.execute(new ExeTask(task));
    }

    /**
     * Puts a task on the queue.  Call on main thread only.
     *
     * @param task
     */
    public void execute(final Task task)
    {
        if(UiThreadContext.isInUiThread())
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

    /**
     * Query existing tasks.  Call on main thread only.
     *
     * @param queueQuery
     */
    public void query(QueueQuery queueQuery)
    {
        UiThreadContext.assertUiThread();

        for (Task task : tasks)
        {
            queueQuery.query(task);
        }

        if(currentTask != null)
            queueQuery.query(currentTask);
    }

    public interface QueueQuery
    {
        void query(Task task);
    }
}
