package co.touchlab.android.threading.tasks;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import co.touchlab.android.threading.tasks.TaskQueue.Task;
import co.touchlab.android.threading.utils.UiThreadContext;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.SubscriberExceptionEvent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by kgalligan on 7/28/14.
 */
public class TaskQueueActual
{
    private final Handler handler;
    private final PollRunnable pollRunnable = new PollRunnable();
    private final PostExeRunnable postExeRunnable = new PostExeRunnable();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
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
    public void execute(Context context, Task task)
    {
        UiThreadContext.assertUiThread();

        //repeatedly assigning seems ugly, but should work.
        application = (Application) context.getApplicationContext();
        tasks.add(task);

        resetPollRunnable();
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
            catch (Exception e)
            {
                boolean handled = task.handleError(e);
                if (!handled)
                    throw new RuntimeException(e);
            }
            finally
            {
                handler.post(postExeRunnable);
            }
        }
    }

    private class PostExeRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();

            if(currentTask != null)
            {
                Task task = currentTask;
                currentTask = null;
                task.onComplete();
            }

            resetPollRunnable();
        }
    }

    private void resetPollRunnable()
    {
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
    }
}
