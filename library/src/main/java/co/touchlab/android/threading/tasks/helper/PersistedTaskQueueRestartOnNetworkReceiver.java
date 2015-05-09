package co.touchlab.android.threading.tasks.helper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import co.touchlab.android.threading.tasks.persisted.PersistedTaskQueue;
import co.touchlab.android.threading.tasks.utils.NetworkUtils;

/**
 * Restarts queue on network change.  Use this with ConnectionChangeBusEventListener to enable/disable
 * this Receiver depending on status of queue.
 *
 * Created by kgalligan on 4/4/15.
 */
public abstract class PersistedTaskQueueRestartOnNetworkReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        boolean online = NetworkUtils.isOnline(context);

        if(online)
        {
            loadQueue().restartQueue();
        }
    }

    protected abstract PersistedTaskQueue loadQueue();
}
