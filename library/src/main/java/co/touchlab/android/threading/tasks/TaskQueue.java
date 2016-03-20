package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import android.os.Message;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import co.touchlab.android.threading.utils.UiThreadContext;

/**
 * Relatively simple queue implementation.  See TaskQueueActual for detail on implementation.
 * <p/>
 * Created by kgalligan on 7/5/14.
 */
public class TaskQueue extends BaseTaskQueue
{
    private static       Map<String, TaskQueue> queueMap      = new HashMap<String, TaskQueue>();
    private static final String                 DEFAULT_QUEUE = "__DEFAULT";
    private static final String                 NETWORK_QUEUE = "__NETWORK";

    /**
     * Get a direct reference to your queue.  Call on main thread.
     *
     * @param name
     * @return
     */
    public static synchronized TaskQueue loadQueue(Context context, String name)
    {
        return loadQueue(context, name, true);
    }

    public static synchronized TaskQueue loadQueue(Context context, String name, boolean fifo)
    {
        TaskQueue taskQueueActual = queueMap.get(name);

        if(taskQueueActual == null)
        {
            taskQueueActual = new TaskQueue((Application) context.getApplicationContext(), fifo);
            queueMap.put(name, taskQueueActual);
        }
        else
        {
            if(taskQueueActual.fifo != fifo)
            {
                throw new IllegalStateException(
                        "Queue already created with different fifo setting: " + name + "/" + fifo);
            }
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

    /**
     * The default queue
     *
     * @return
     */
    public static TaskQueue loadQueueNetwork(Context context)
    {
        return loadQueue(context, NETWORK_QUEUE);
    }

    //*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~
    //*~*~*~*~*~*~*~*~*~*~ PER INSTANCE *~*~*~*~*~*~*~*~*~*~*~*~*~
    //*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~
    private final boolean fifo;

    public TaskQueue(Application application)
    {
        this(application, true);
    }

    public TaskQueue(Application application, boolean fifo)
    {
        super(application, new LinkedListQueue<Task>(fifo));
        this.fifo = fifo;
    }

    static class LinkedListQueue <Task> implements QueueWrapper<Task>
    {
        private final LinkedList<Task> linkedList = new LinkedList<Task>();
        private final boolean fifo;

        public LinkedListQueue(boolean fifo)
        {
            this.fifo = fifo;
        }

        @Override
        public Task poll()
        {
            return fifo
                    ? linkedList.poll()
                    : linkedList.pollLast();
        }

        @Override
        public void offer(Task task)
        {
            linkedList.offer(task);
        }

        @Override
        public Collection<Task> all()
        {
            return linkedList;
        }

        @Override
        public void remove(Task task)
        {
            linkedList.remove(task);
        }
    }

    @Override
    protected void runTask(Task task)
    {
        executeHandler.post(new ExeTask(task));
    }

    @Override
    protected void finishTask(Message msg, Task task)
    {
        try
        {
            if(task != null)
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
        task.setMyQueue(this);
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
            catch(Throwable e)
            {
                boolean handled = task.handleError(application, e);
                if(! handled)
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
