package co.touchlab.android.threading.tasks.helper;
import android.content.Context;
import android.os.PowerManager;

import co.touchlab.android.threading.tasks.BaseTaskQueue;
import co.touchlab.android.threading.tasks.Task;

/**
 * Runs the queue in a wake lock.  If you don't understand why this might be dangerous, don't
 * use it.
 *
 * Created by kgalligan on 4/4/15.
 */
public class WakeLockQueueListener implements BaseTaskQueue.QueueListener
{
    public static final String PERSISTED_WAKELOCK = "PERSISTED_WAKELOCK";
    private final Context app;
    private PowerManager.WakeLock wakeLock;

    public WakeLockQueueListener(Context app)
    {
        this.app = app.getApplicationContext();
    }

    @Override
    public void queueStarted(BaseTaskQueue queue)
    {
        if(wakeLock != null)
            throw new RuntimeException("Something is way wrong with the config");

        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getWakelockName());
        wakeLock.acquire();
    }

    @Override
    public void queueFinished(BaseTaskQueue queue)
    {
        wakeLock.release();
        wakeLock = null;
    }

    @Override
    public void taskStarted(BaseTaskQueue queue, Task task)
    {

    }

    @Override
    public void taskFinished(BaseTaskQueue queue, Task task)
    {

    }

    /**
     * Override if you need something funky
     *
     * @return
     */
    protected String getWakelockName()
    {
        return PERSISTED_WAKELOCK;
    }
}
