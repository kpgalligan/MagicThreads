package co.touchlab.android.threading.tasks;

import android.content.Context;

/**
 * Created by kgalligan on 10/26/14.
 */
public abstract class Task
{
    protected transient BaseTaskQueue myQueue;

    public void setMyQueue(BaseTaskQueue myQueue)
    {
        this.myQueue = myQueue;
    }

    protected abstract void run(Context context) throws Throwable;

    /**
     * Handle Exception that occurred during processing.  Return true if handled, false if not.  If not handled, app will throw and probably crash.
     *
     * @param e
     * @return true if handled, false if not (which will throw it)
     */
    protected abstract boolean handleError(Context context, Throwable e);

    /**
     * Post result to EventBus (or whatever).  This will happen after all queue orchestration is
     * complete.
     */
    protected void onComplete(Context context)
    {

    }
}
