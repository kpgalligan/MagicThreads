package co.touchlab.android.threading.loaders;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * Created by kgalligan on 7/4/14.
 */
public class SmoothLoaderBroadcastReceiver extends LoaderBroadcastReceiver
{
    private final Handler handler;
    private final ContentChangedRunnable contentChangedRunnable;

    public SmoothLoaderBroadcastReceiver(AbstractLocalBroadcastReceiverLoader loader)
    {
        super(loader);
        handler = new Handler();
        contentChangedRunnable = new ContentChangedRunnable();
    }

    class ContentChangedRunnable implements Runnable
    {
        @Override
        public void run()
        {
            getLoader().onContentChanged();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        handler.removeCallbacks(contentChangedRunnable);
        handler.postDelayed(contentChangedRunnable, 300);
    }
}
