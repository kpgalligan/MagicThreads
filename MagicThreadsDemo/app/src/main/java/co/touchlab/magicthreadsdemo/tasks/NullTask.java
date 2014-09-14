package co.touchlab.magicthreadsdemo.tasks;

import android.content.Context;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;

/**
 * This does nothing, really.
 *
 * Created by kgalligan on 9/13/14.
 */
public class NullTask extends TaskQueue.Task
{
    @Override
    protected void run(Context context) throws Exception
    {
        Thread.sleep(5000);
    }

    @Override
    protected void onComplete()
    {
        EventBusExt.getDefault().post(this);
    }

    @Override
    protected boolean handleError(Exception e)
    {
        return false;
    }


}
