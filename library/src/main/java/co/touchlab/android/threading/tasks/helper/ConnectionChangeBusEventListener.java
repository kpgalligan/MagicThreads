package co.touchlab.android.threading.tasks.helper;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import co.touchlab.android.threading.tasks.BaseTaskQueue;
import co.touchlab.android.threading.tasks.Task;

/**
 * Toggle network listener.  If there are no tasks waiting, we shouldn't keep restarting.
 *
 * Created by kgalligan on 4/4/15.
 */
public class ConnectionChangeBusEventListener implements BaseTaskQueue.QueueListener
{
    private final Context app;
    private final Class receiverClass;

    public ConnectionChangeBusEventListener(Context app, Class receiverClass)
    {
        this.app = app;
        this.receiverClass = receiverClass;
    }

    @Override
    public void queueStarted(BaseTaskQueue queue)
    {

    }

    @Override
    public void queueFinished(BaseTaskQueue queue)
    {
        boolean complete = queue.countTasks() == 0;
        int flag=(complete ?
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        ComponentName component = new ComponentName(app, receiverClass);

        app.getPackageManager().setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP);
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
