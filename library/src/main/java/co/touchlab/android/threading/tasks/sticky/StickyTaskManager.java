package co.touchlab.android.threading.tasks.sticky;

import android.os.Bundle;
import co.touchlab.android.threading.utils.UiThreadContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kgalligan on 10/26/14.
 */
public class StickyTaskManager
{
    private final static AtomicInteger idCounter = new AtomicInteger();
    public static final String CONTEXT_ID = "CONTEXT_ID";

    protected final int affinityId;

    public StickyTaskManager(Bundle inState)
    {
        UiThreadContext.assertUiThread();

        if(inState == null || !inState.containsKey(CONTEXT_ID))
            affinityId = idCounter.incrementAndGet();
        else
            affinityId = inState.getInt(CONTEXT_ID);
    }

    public void onSaveInstanceState(Bundle outState)
    {
        UiThreadContext.assertUiThread();
        outState.putInt(CONTEXT_ID, affinityId);
    }

    public boolean isTaskForMe(StickyTask stickyTask)
    {
        return stickyTask.affinityId == affinityId;
    }
}
