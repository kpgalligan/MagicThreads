package co.touchlab.android.threading.tasks.sticky;

import android.os.Bundle;

import java.util.concurrent.atomic.AtomicInteger;

import co.touchlab.android.threading.utils.UiThreadContext;

/**
 * Created by kgalligan on 10/26/14.
 */
public class StickyTaskManager
{
    private static long idCounter = System.currentTimeMillis();
    public static final String CONTEXT_ID = "CONTEXT_ID";

    protected final long affinityId;

    public StickyTaskManager(Bundle inState)
    {
        UiThreadContext.assertUiThread();

        if (inState == null || !inState.containsKey(CONTEXT_ID))
            affinityId = idCounter++;
        else
            affinityId = inState.getInt(CONTEXT_ID);
    }

    public void onSaveInstanceState(Bundle outState)
    {
        UiThreadContext.assertUiThread();
        outState.putLong(CONTEXT_ID, affinityId);
    }

    public boolean isTaskForMe(StickyTask stickyTask)
    {
        return stickyTask.affinityId == affinityId;
    }
}
