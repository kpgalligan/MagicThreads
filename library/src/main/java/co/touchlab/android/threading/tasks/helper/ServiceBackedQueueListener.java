package co.touchlab.android.threading.tasks.helper;
import android.content.Context;
import android.content.Intent;

import co.touchlab.android.threading.tasks.BaseTaskQueue;
import co.touchlab.android.threading.tasks.Task;

/**
 * Run tasks with a service started.  Service does nothing.  However, since its running, your app is
 * more likely to stick around.
 *
 * Created by kgalligan on 4/4/15.
 */
public class ServiceBackedQueueListener implements BaseTaskQueue.QueueListener
{
    private final Context app;
    private final Class serviceClass;

    public ServiceBackedQueueListener(Context app, Class serviceClass)
    {
        this.serviceClass = serviceClass;
        this.app = app.getApplicationContext();
    }

    @Override
    public void queueStarted(BaseTaskQueue queue)
    {
        app.startService(makeServiceIntent());
    }

    private Intent makeServiceIntent()
    {
        return new Intent(app, serviceClass);
    }

    @Override
    public void queueFinished(BaseTaskQueue queue)
    {
        app.stopService(makeServiceIntent());
    }

    @Override
    public void taskStarted(BaseTaskQueue queue, Task task)
    {

    }

    @Override
    public void taskFinished(BaseTaskQueue queue, Task task)
    {

    }
}
