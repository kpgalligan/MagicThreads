package co.touchlab.android.threading.tasks;

import android.content.Context;
import android.os.Bundle;
import co.touchlab.android.threading.utils.UiThreadContext;
import de.greenrobot.event.EventBus;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages tasks and affinity to hosts.  Should survive config changes such as rotations,
 * but you MUST pass the bundle from onCreate, and you MUST call onSaveInstanceState here
 * from onSaveInstanceState in your Fractivity.
 *
 * From other contexts, like a service, you can simply pass null to the constructor.  We don't
 * have an alternate constructor to make it clear to users that if you're in a Fractivity,
 * you should pass in the bundle, unless you REALLY don't want rotations to hold onto tasks.
 *
 * Created by kgalligan on 7/6/14.
 */
public class BsyncTaskManager<D>
{
    private final static AtomicInteger idCounter = new AtomicInteger();
    public static final String CONTEXT_ID = "CONTEXT_ID";

    private final int contextId;
    private D host;

    public BsyncTaskManager(Bundle inState)
    {
        UiThreadContext.assertUiThread();

        if(inState == null || !inState.containsKey(CONTEXT_ID))
            contextId = idCounter.incrementAndGet();
        else
            contextId = inState.getInt(CONTEXT_ID);
    }

    public void register(D host)
    {
        this.host = host;
        UiThreadContext.assertUiThread();
        EventBus.getDefault().register(this);
    }

    public void unregister()
    {
        this.host = null;
        UiThreadContext.assertUiThread();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(BsyncTask bsyncTask)
    {
        UiThreadContext.assertUiThread();
        if(contextId == bsyncTask.contextId)
            bsyncTask.onPostExecute(host);
    }

    public void post(Context context, BsyncTask task)
    {
        task.contextId = contextId;
        TaskQueue.execute(context, task);
    }

    public void onSaveInstanceState(Bundle outState)
    {
        UiThreadContext.assertUiThread();
        outState.putInt(CONTEXT_ID, contextId);
    }

}
