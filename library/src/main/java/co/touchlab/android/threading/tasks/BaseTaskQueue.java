package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.touchlab.android.threading.utils.UiThreadContext;

/**
 * Created by kgalligan on 11/30/14.
 */
public abstract class BaseTaskQueue
{
    protected Application application;
    protected final Handler handler;
    protected Queue<Task> tasks;
    private Task currentTask;

    protected final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(r);
        }
    });


    public BaseTaskQueue(Application application)
    {
        this.application = application;
        handler = new QueueHandler(Looper.getMainLooper());
        tasks = createQueue();
    }

    protected void insertTask(Task task)
    {
        UiThreadContext.assertUiThread();

        tasks.offer(task);

        resetPollRunnable();
    }

    protected void resetPollRunnable()
    {
        handler.removeMessages(QueueHandler.POLL_TASK);
        handler.sendMessage(handler.obtainMessage(QueueHandler.POLL_TASK));
    }

    protected class QueueHandler extends Handler
    {
        static final int INSERT_TASK = 0;
        static final int POLL_TASK = 1;
        public static final int POST_EXE = 2;
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
                case INSERT_TASK:
                    insertTask((Task) msg.obj);
                    break;
                case POLL_TASK:
                    if (currentTask != null)
                        return;

                    Task task = tasks.poll();
                    if (task != null)
                    {
                        currentTask = task;
                        runTask(task);
                    }
                    break;
                case POST_EXE:
                    Task tempTask = currentTask;
                    currentTask = null;
                    finishTask(msg, tempTask);
                    break;
                case THROW:
                    Throwable cause = (Throwable) msg.obj;
                    if (cause instanceof RuntimeException)
                        throw (RuntimeException) cause;
                    else if (cause instanceof Error)
                        throw (Error) cause;
                    else
                        throw new RuntimeException(cause);
                default:
                    otherOperations(msg);
            }
        }
    }

    public TaskQueueState copyState()
    {
        UiThreadContext.assertUiThread();

        PriorityQueue<Task> commands = new PriorityQueue<Task>(tasks);
        List<Task> commandList = new ArrayList<Task>();
        while (!commands.isEmpty())
        {
            commandList.add(commands.poll());
        }
        return new TaskQueueState(commandList, currentTask);
    }

    public static class TaskQueueState
    {
        List<Task> queued;
        Task currentTask;

        public TaskQueueState(List<Task> queued, Task currentTask)
        {
            this.queued = queued;
            this.currentTask = currentTask;
        }

        public List<Task> getQueued()
        {
            return queued;
        }

        public Task getCurrentTask()
        {
            return currentTask;
        }
    }

    protected abstract Queue<Task> createQueue();

    protected abstract void runTask(Task task);

    protected abstract void finishTask(Message msg, Task task);

    protected void otherOperations(Message msg)
    {

    }

    public interface QueueQuery
    {
        void query(Task task);
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

        if (currentTask != null)
            queueQuery.query(currentTask);
    }
}
