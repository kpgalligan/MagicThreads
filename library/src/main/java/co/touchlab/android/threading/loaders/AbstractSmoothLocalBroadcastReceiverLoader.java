package co.touchlab.android.threading.loaders;

import android.content.Context;

/**
 * Loader that attempts to reduce the volume of reloads.  If in a situation where lots of refresh
 * notifications may come in, wait a bit before actually reloading.
 * <p/>
 * Created by kgalligan on 7/4/14.
 */
public abstract class AbstractSmoothLocalBroadcastReceiverLoader<D> extends AbstractLocalBroadcastReceiverLoader<D>
{
    public AbstractSmoothLocalBroadcastReceiverLoader(Context context)
    {
        super(context);
    }

    @Override
    protected LoaderBroadcastReceiver createLoaderBroadcastReceiver()
    {
        return new SmoothLoaderBroadcastReceiver(this);
    }
}
