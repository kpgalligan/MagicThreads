package co.touchlab.android.threading.loaders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * Created by kgalligan on 7/4/14.
 */
public class LoaderBroadcastReceiver extends BroadcastReceiver
{
    private AbstractLocalBroadcastReceiverLoader loader;

    public LoaderBroadcastReceiver(AbstractLocalBroadcastReceiverLoader loader)
    {
        this.loader = loader;
    }

    public AbstractLocalBroadcastReceiverLoader getLoader()
    {
        return loader;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        getLoader().onContentChanged();
    }
}
