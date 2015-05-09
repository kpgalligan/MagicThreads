package co.touchlab.android.threading.tasks.helper;
import android.content.Context;

import co.touchlab.android.threading.errorcontrol.SoftException;
import co.touchlab.android.threading.tasks.persisted.PersistedTask;

/**
 * For apps that have lots of eyeballs monitoring them, and are more worried about losing data than wedging themselves, try this.
 *
 * It'll basically never throw away a task.  This is only OK if you're watching the error logs religiously, and pushing app fixes as
 * errors are seen.  If you never let persisted tasks to fail, they run the risk of permanently wedging your app.  However,
 * if you're on top of app fixes, this might be the better option.
 *
 * Created by kgalligan on 4/4/15.
 */
public abstract class NeverFailPersistedTask extends PersistedTask
{
    @Override
    protected final void run(Context context) throws SoftException, Throwable
    {
        try
        {
            runTask(context);
        }
        catch(Exception e)
        {
            if(e instanceof SoftException)
                throw e;
            else
                reportError(e);
        }
    }

    /**
     * Put your logic here.  Exceptions are reported, but never fail the task.
     *
     * @param context
     */
    protected abstract void runTask(Context context)throws Exception;

    /**
     * When an error is found, this method reports it.  You *must* send errors somewhere, and fix/redeploy the app.
     * If you don't, your app will forever wedge.
     * If you don't report your error here, your app will be a mess.  You have been warned.
     *
     * @param e
     */
    protected abstract void reportError(Exception e);

    @Override
    protected boolean handleError(Context context, Throwable e)
    {
        return false;
    }
}
