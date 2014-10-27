package co.touchlab.android.threading.tasks;

import android.content.Context;

/**
 * Created by kgalligan on 10/26/14.
 */
public abstract class Task
{
    private long affinityId;

    protected abstract void run(Context context) throws Exception;

    /**
     * Handle Exception that occurred during processing.  Return true if handled, false if not.  If not handled, app will throw and probably crash.
     *
     * @param e
     * @return true if handled, false if not (which will throw it)
     */
    protected abstract boolean handleError(Throwable e);

    /**
     * Post result to EventBus (or whatever).  This will happen after all queue orchestration is
     * complete.
     */
    protected void onComplete(Context context)
    {

    }
}
