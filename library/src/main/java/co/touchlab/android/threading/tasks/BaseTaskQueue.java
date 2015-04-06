package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.touchlab.android.threading.utils.UiThreadContext;

/**
 * Created by kgalligan on 11/30/14.
 */
public abstract class BaseTaskQueue
{
    protected final Application        application;
    protected final Handler            handler;
    protected final QueueWrapper<Task> tasks;
    private         Task               currentTask;

    protected final ExecutorService executorService = Executors
            .newSingleThreadExecutor(new ThreadFactory()
            {
                @Override
                public Thread newThread(Runnable r)
                {
                    return new Thread(r);
                }
            });

    private List<QueueListener> listeners     = new ArrayList<QueueListener>();
    private boolean             startedCalled = false;

    public BaseTaskQueue(Application application, QueueWrapper<Task> queueWrapper)
    {
        this.application = application;
        tasks = queueWrapper;
        handler = new QueueHandler(Looper.getMainLooper());
    }

    protected interface QueueWrapper <T>
    {
        T poll();

        void offer(T task);

        Collection<T> all();

        void remove(T task);
    }

    public int countTasks()
    {
        return tasks.all().size() + (currentTask == null ? 0 : 1);
    }

    public void addListener(QueueListener listener)
    {
        listeners.add(listener);
    }

    public void clearListeners()
    {
        listeners.clear();
    }

    protected void insertTask(Task task)
    {
        UiThreadContext.assertUiThread();

        tasks.offer(task);

        resetPollRunnable();
    }

    public void remove(Task task)
    {
        tasks.remove(task);
    }

    protected void resetPollRunnable()
    {
        handler.removeMessages(QueueHandler.POLL_TASK);
        handler.sendMessage(handler.obtainMessage(QueueHandler.POLL_TASK));
    }

    protected class QueueHandler extends Handler
    {
        static final        int INSERT_TASK = 0;
        static final        int POLL_TASK   = 1;
        public static final int POST_EXE    = 2;
        static final        int THROW       = 3;

        private QueueHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case INSERT_TASK:
                    insertTask((Task) msg.obj);
                    break;
                case POLL_TASK:
                    if(currentTask != null)
                    {
                        return;
                    }

                    Task task = tasks.poll();
                    if(task != null)
                    {
                        currentTask = task;
                        if(! startedCalled)
                        {
                            startedCalled = true;
                            for(QueueListener listener : listeners)
                            {
                                listener.queueStarted(BaseTaskQueue.this);
                            }
                        }
                        for(QueueListener listener : listeners)
                        {
                            listener.taskStarted(BaseTaskQueue.this, task);
                        }
                        runTask(task);
                    }
                    else
                    {
                        callQueueFinished();
                    }
                    break;
                case POST_EXE:
                    Task tempTask = currentTask;
                    currentTask = null;
                    finishTask(msg, tempTask);
                    for(QueueListener listener : listeners)
                    {
                        listener.taskFinished(BaseTaskQueue.this, tempTask);
                    }
                    break;
                case THROW:
                    Throwable cause = (Throwable) msg.obj;
                    if(cause instanceof RuntimeException)
                    {
                        throw (RuntimeException) cause;
                    }
                    else if(cause instanceof Error)
                    {
                        throw (Error) cause;
                    }
                    else
                    {
                        throw new RuntimeException(cause);
                    }
                default:
                    otherOperations(msg);
            }
        }
    }

    protected void callQueueFinished()
    {
        for(QueueListener listener : listeners)
        {
            listener.queueFinished(this);
        }
        startedCalled = false;
    }

    public TaskQueueState copyState()
    {
        UiThreadContext.assertUiThread();

        PriorityQueue<Task> commands = new PriorityQueue<Task>(tasks.all());
        List<Task> commandList = new ArrayList<Task>();
        while(! commands.isEmpty())
        {
            commandList.add(commands.poll());
        }
        return new TaskQueueState(commandList, currentTask);
    }

    public static class TaskQueueState
    {
        List<Task> queued;
        Task       currentTask;

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

    protected abstract void runTask(Task task);

    protected abstract void finishTask(Message msg, Task task);

    protected void otherOperations(Message msg)
    {

    }

    public interface QueueQuery
    {
        void query(BaseTaskQueue queue, Task task);
    }

    public interface QueueListener
    {
        void queueStarted(BaseTaskQueue queue);

        void queueFinished(BaseTaskQueue queue);

        void taskStarted(BaseTaskQueue queue, Task task);

        void taskFinished(BaseTaskQueue queue, Task task);
    }

    /**
     * Query existing tasks.  Call on main thread only.
     *
     * @param queueQuery
     */
    public void query(QueueQuery queueQuery)
    {
        UiThreadContext.assertUiThread();

        for(Task task : tasks.all())
        {
            queueQuery.query(this, task);
        }

        if(currentTask != null)
        {
            queueQuery.query(this, currentTask);
        }
    }
}
