package co.touchlab.android.threading.tasks.persisted.storage;

import android.app.Application;
import android.content.Context;
import co.touchlab.android.threading.tasks.persisted.ConfigException;
import co.touchlab.android.threading.tasks.persisted.PersistedTaskQueue;
import co.touchlab.android.threading.tasks.persisted.PersistedTaskQueueConfig;

/**
 * Created by kgalligan on 10/4/14.
 */
public class DefaultPersistedTaskQueue
{
    private static PersistedTaskQueue INSTANCE;

    public static synchronized PersistedTaskQueue getInstance(Context context)
    {
        if (INSTANCE == null)
        {
            PersistedTaskQueueConfig build = null;
            try
            {
                build = new PersistedTaskQueueConfig.Builder().build(context);
            } catch (ConfigException e)
            {
                throw new RuntimeException(e);
            }
            INSTANCE = new PersistedTaskQueue((Application)context.getApplicationContext(), build);
        }

        return INSTANCE;
    }
}
