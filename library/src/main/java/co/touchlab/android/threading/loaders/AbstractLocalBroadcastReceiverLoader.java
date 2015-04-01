package co.touchlab.android.threading.loaders;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Loader that listens to the local broadcasts for refresh notifications.
 * <p/>
 * Created by kgalligan on 7/4/14.
 */
public abstract class AbstractLocalBroadcastReceiverLoader <D> extends AbstractDataLoader<D>
{
    private LoaderBroadcastReceiver loaderBroadcastReceiver = null;

    public AbstractLocalBroadcastReceiverLoader(Context context)
    {
        super(context);
    }

    protected String getBroadcastString()
    {
        return null;
    }

    protected String[] getBroadcastStrings()
    {
        String broadcastString = getBroadcastString();
        return broadcastString == null
                ? new String[] {}
                : new String[] {broadcastString};
    }

    @Override
    protected void registerContentChangedObserver()
    {
        String[] broadcastStrings = getBroadcastStrings();
        if(loaderBroadcastReceiver == null && broadcastStrings.length > 0)
        {
            loaderBroadcastReceiver = createLoaderBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            for(String s : broadcastStrings)
            {
                filter.addAction(s);
            }
            LocalBroadcastManager.getInstance(getContext())
                                 .registerReceiver(loaderBroadcastReceiver, filter);
        }
    }

    protected LoaderBroadcastReceiver createLoaderBroadcastReceiver()
    {
        return new LoaderBroadcastReceiver(this);
    }

    @Override
    protected void unregisterContentChangedObserver()
    {
        if(loaderBroadcastReceiver != null)
        {
            LocalBroadcastManager.getInstance(getContext())
                                 .unregisterReceiver(loaderBroadcastReceiver);
            loaderBroadcastReceiver = null;
        }
    }
}
