package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import co.touchlab.android.threading.utils.UiThreadContext;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by kgalligan on 11/30/14.
 */
public abstract class BaseTaskQueue
{
    protected Application application;
    protected final Handler handler;
    private Queue<Task> tasks = new LinkedList<Task>();
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
    }

    protected void insertTask(Task task)
    {
        UiThreadContext.assertUiThread();

        tasks.add(task);

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
                case INSERT_TASK:
                    insertTask((Task)msg.obj);
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

    protected abstract void runTask(Task task);

}
