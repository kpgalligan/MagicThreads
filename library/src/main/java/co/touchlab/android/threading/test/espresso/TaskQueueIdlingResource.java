package co.touchlab.android.threading.test.espresso;

import android.os.Handler;
import android.os.Looper;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.android.threading.tasks.TaskQueueActual;
import co.touchlab.android.threading.tasks.utils.TaskQueueHelper;
import com.google.android.apps.common.testing.ui.espresso.IdlingResource;
import com.sun.org.apache.regexp.internal.RECompiler;

/**
 * Created by kgalligan on 10/12/14.
 */
public class TaskQueueIdlingResource implements IdlingResource
{
    Class[] tasks;
    TaskQueueActual taskQueueActual;
    ResourceCallback resourceCallback;
    private final Handler handler;

    CheckIdleRunnable checkIdleRunnable = new CheckIdleRunnable();

    class CheckIdleRunnable implements Runnable
    {
        @Override
        public void run()
        {
            if(isIdleNow())
                resourceCallback.onTransitionToIdle();
            else
                handler.postDelayed(checkIdleRunnable, 500);
        }
    }

    public TaskQueueIdlingResource(TaskQueueActual taskQueueActual, Class... tasks)
    {
        this.taskQueueActual = taskQueueActual;
        this.tasks = tasks;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public String getName()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TaskQueueIdlingResource.class.getSimpleName());
        stringBuilder.append("-");
        for (Class task : tasks)
        {
            stringBuilder.append(task.getSimpleName()).append("|");
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean isIdleNow()
    {
        return !TaskQueueHelper.hasTasksOfType(taskQueueActual, tasks);
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback)
    {
        this.resourceCallback = resourceCallback;
        handler.post(checkIdleRunnable);
    }
}
