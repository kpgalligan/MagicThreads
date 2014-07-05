package co.touchlab.android.threading.loaders;

import android.content.Context;

/**
 * Created by kgalligan on 7/4/14.
 */
public abstract class AbstractSmoothLocalBroadcastReceiverLoader extends AbstractLocalBroadcastReceiverLoader
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
