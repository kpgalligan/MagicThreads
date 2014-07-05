package co.touchlab.android.threading.loaders;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by kgalligan on 7/4/14.
 */
public abstract class BaseAsyncLoader<D> extends AsyncTaskLoader<D>
{
    private D data;
    private LoaderBroadcastReceiver loaderBroadcastReceiver = null;

    public BaseAsyncLoader(Context context)
    {
        super(context);
    }

    @Override
    protected void onStartLoading()
    {
        if (data != null)
        {
            deliverResult(data);
        }

        String[] broadcastStrings = getBroadcastStrings();
        if (loaderBroadcastReceiver == null && broadcastStrings.length > 0)
        {
            loaderBroadcastReceiver = new LoaderBroadcastReceiver(this);
            IntentFilter filter = new IntentFilter();
            for (String s : broadcastStrings)
            {
                filter.addAction(s);
            }
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(loaderBroadcastReceiver, filter);
        }

        if (takeContentChanged() || data == null)
        {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(D data)
    {
        if (isReset())
        {
            if (data != null)
            {
                onReleaseResources(data);
            }
        }

        D oldData = this.data;
        this.data = data;

        if (isStarted() && data != null)
        {
            super.deliverResult(data);
        }

        if (oldData != null)
        {
            onReleaseResources(oldData);
        }
    }

    @Override
    protected void onStopLoading()
    {
        cancelLoad();
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        onStopLoading();

        if (data != null)
        {
            onReleaseResources(data);
            data = null;
        }

        if (loaderBroadcastReceiver != null)
        {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(loaderBroadcastReceiver);
            loaderBroadcastReceiver = null;
        }
    }

    protected void onReleaseResources(D data)
    {
    }

    protected String getBroadcastString()
    {
        return null;
    }

    protected String[] getBroadcastStrings()
    {
        String broadcastString = getBroadcastString();
        return broadcastString == null ? new String[]{} : new String[]{broadcastString};
    }


}