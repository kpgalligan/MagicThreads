package co.touchlab.android.threading.tasks;

import de.greenrobot.event.EventBus;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kgalligan on 7/6/14.
 */
public class BsyncTaskManager
{
    private final static AtomicInteger idCounter = new AtomicInteger();

    private final int contextId;

    public BsyncTaskManager()
    {
        contextId = idCounter.incrementAndGet();
    }

    public void register()
    {
        EventBus.getDefault().register(this);
    }

    public void unregister()
    {
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(BsyncTask bsyncTask)
    {
        if(contextId == bsyncTask.contextId)
            bsyncTask.onPostExecute();
    }

    public void post(BsyncTask task)
    {
        task.contextId = contextId;
        TaskQueue.execute(task);
    }
}
