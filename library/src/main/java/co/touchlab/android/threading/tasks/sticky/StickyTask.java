package co.touchlab.android.threading.tasks.sticky;

import co.touchlab.android.threading.tasks.Task;

/**
 * Created by kgalligan on 10/26/14.
 */
public abstract class StickyTask extends Task
{
    protected final long affinityId;

    protected StickyTask(StickyTaskManager taskManager)
    {
        affinityId = taskManager.affinityId;
    }
}
