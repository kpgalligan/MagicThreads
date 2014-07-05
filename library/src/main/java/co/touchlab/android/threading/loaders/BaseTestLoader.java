package co.touchlab.android.threading.loaders;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Created by kgalligan on 7/4/14.
 */
public abstract class BaseTestLoader<D> extends AsyncTaskLoader<D>
{
    private D data;

    public BaseTestLoader(Context context)
    {
        super(context);
    }

    /**
     * Starts an asynchronous load of the data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     * <p/>
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading()
    {
        if (data != null)
        {
            deliverResult(data);
        }

        if (takeContentChanged() || data == null)
        {
            forceLoad();
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(D newData)
    {
        if (isReset())
        {
            return;
        }

        D oldData = this.data;
        data = newData;

        if (isStarted())
        {
            super.deliverResult(newData);
        }

        if (oldData != null)
        {
            onReleaseResources(oldData);
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading()
    {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (data != null)
        {
            onReleaseResources(data);
            data = null;
        }

        unregisterContentChangedObserver();
    }

    /* Runs on a worker thread */
    @Override
    public D loadInBackground()
    {
        D localData = findContent();

        if (localData != null)
        {
            registerContentChangedObserver();
        }

        return localData;
    }

    protected abstract D findContent();

    protected abstract void registerContentChangedObserver();
    protected abstract void unregisterContentChangedObserver();

    protected void onReleaseResources(D data)
    {
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args)
    {
        super.dump(prefix, fd, writer, args);

        writer.print(prefix);
        writer.print("data=");
        writer.println(data);
    }
}
