package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import co.touchlab.android.threading.tasks.TaskQueue.Task;
import co.touchlab.android.threading.utils.UiThreadContext;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.SubscriberExceptionEvent;

import javax.swing.text.html.HTMLDocument;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * Created by kgalligan on 7/28/14.
 */
public class TaskQueueActual
{
    private final Handler handler;
    private final PollRunnable pollRunnable = new PollRunnable();
    private final PostExeRunnable postExeRunnable = new PostExeRunnable();
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

    public TaskQueueActual()
    {
        handler = new Handler(Looper.getMainLooper());
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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callExecute(context, task);
                }
            });
        }
    }

    private void callExecute(Context context, Task task)
    {
        UiThreadContext.assertUiThread();

        //repeatedly assigning seems ugly, but should work.
        application = (Application) context.getApplicationContext();
        tasks.add(task);

        resetPollRunnable();
    }

    private void resetPollRunnable()
    {
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
    }

    private class PollRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();

            Task task = tasks.poll();
            if (task != null)
            {
                currentTask = task;
                executorService.execute(new ExeTask(task));
            }
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
                boolean handled = task.handleError(e);
                if (!handled)
                    handler.post(new ThrowRunnable(e));
            }
            finally
            {
                handler.post(postExeRunnable);
            }
        }
    }

    private class ThrowRunnable implements Runnable
    {
        private Throwable cause;

        private ThrowRunnable(Throwable cause)
        {
            this.cause = cause;
        }

        @Override
        public void run()
        {
            if(cause instanceof RuntimeException)
                throw (RuntimeException)cause;
            else if(cause instanceof Error)
                throw (Error)cause;
            else
                throw new RuntimeException(cause);
        }
    }

    private class PostExeRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();

            try
            {
                if(currentTask != null)
                {
                    Task task = currentTask;
                    currentTask = null;
                    task.onComplete(application);
                }
            }
            finally
            {
                resetPollRunnable();
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
