package co.touchlab.android.threading.tasks.persisted;

import android.content.Context;

/**
 * Created by kgalligan on 10/4/14.
 */
public interface SuperbusEventListener
{
    void onBusStarted(Context context, PersistenceProvider provider);
    void onBusFinished(Context context, PersistenceProvider provider, boolean complete);

    //Should probably add something for each command
}
