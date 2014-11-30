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
public class TaskQueue
{
    private static Map<String, TaskQueue> queueMap = new HashMap<String, TaskQueue>();
    private static final String DEFAULT_QUEUE = "__DEFAULT";

    /**
     * Get a direct reference to your queue.  Call on main thread.
     * @param name
     * @return
     */
    public static synchronized TaskQueue loadQueue(String name)
    {
        TaskQueue taskQueueActual = queueMap.get(name);
        if(taskQueueActual == null)
        {
            taskQueueActual = new TaskQueue();
            queueMap.put(name, taskQueueActual);
        }

        return taskQueueActual;
    }

    /**
     * The default queue
     * @return
     */
    public static TaskQueue loadQueueDefault()
    {
        return loadQueue(DEFAULT_QUEUE);
    }


    /**
     * Puts a task on the queue. Use for local ops. Faster responses.
     *
     * @param context
     * @param task
     */
    public static void execute(Context context, String queue, Task task)
    {
        TaskQueue queueActual = loadQueue(queue);
        queueActual.execute(context, task);
    }

    //*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~
    //*~*~*~*~*~*~*~*~*~*~ PER INSTANCE *~*~*~*~*~*~*~*~*~*~*~*~*~
    //*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~

    private final Handler handler;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(r);
        }
    });
    private Queue<Task> tasks = new LinkedList<Task>();
    private Task currentTask;
    private Application application;

    public TaskQueue()
    {
        handler = new QueueHandler(Looper.getMainLooper());
    }

    private class QueueHandler extends Handler
    {
        static final int CALL_EXECUTE = 0;
        static final int POLL_TASK = 1;
        static final int POST_EXE = 2;
        static final int THROW = 3;

        private QueueHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case CALL_EXECUTE:
                    CallExecutePackage executePackage = (CallExecutePackage) msg.obj;
                    callExecute(executePackage.context, executePackage.task);
                    break;
                case POLL_TASK:
                    if (currentTask != null)
                        return;

                    Task task = tasks.poll();
                    if (task != null)
                    {
                        currentTask = task;
                        executorService.execute(new ExeTask(task));
                    }
                    break;
                case POST_EXE:
                    try
                    {
                        if(currentTask != null)
                        {
                            Task tempTask = currentTask;
                            currentTask = null;
                            tempTask.onComplete(application);
                        }
                    }
                    finally
                    {
                        resetPollRunnable();
                    }
                    break;
                case THROW:
                    Throwable cause = (Throwable)msg.obj;
                    if(cause instanceof RuntimeException)
                        throw (RuntimeException)cause;
                    else if(cause instanceof Error)
                        throw (Error)cause;
                    else
                        throw new RuntimeException(cause);
            }
        }
    }
    /**
     * Puts a task on the queue.  Call on main thread only.
     *
     * @param context
     * @param task
     */
    public void execute(final Context context, final Task task)
    {
        if(UiThreadContext.isInUiThread())
        {
            callExecute(context, task);
        }
        else
        {
            Message message = handler.obtainMessage(QueueHandler.CALL_EXECUTE, new CallExecutePackage(context, task));
            handler.sendMessage(message);
        }
    }

    private static class CallExecutePackage
    {
        public final Context context;
        public final Task task;

        private CallExecutePackage(Context context, Task task)
        {
            this.context = context;
            this.task = task;
        }
    }

    private void callExecute(Context context, Task task)
    {
        UiThreadContext.assertUiThread();

        if(application == null)
            application = (Application) context.getApplicationContext();

        tasks.add(task);

        resetPollRunnable();
    }

    private void resetPollRunnable()
    {
        handler.removeMessages(QueueHandler.POLL_TASK);
        handler.sendMessage(handler.obtainMessage(QueueHandler.POLL_TASK));
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
                boolean handled = task.handleError(e);
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
