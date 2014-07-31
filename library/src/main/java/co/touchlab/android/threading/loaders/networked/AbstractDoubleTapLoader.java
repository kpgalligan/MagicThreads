package co.touchlab.android.threading.loaders.networked;

import android.content.Context;
import co.touchlab.android.threading.loaders.AbstractDataLoader;
import co.touchlab.android.threading.tasks.BsyncTask;
import co.touchlab.android.threading.tasks.BsyncTaskManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Will check locally and trigger a remote call.  You are responsible for saving the remote response, such that on a future call, it will
 * pull the locally available result.
 * <p/>
 * If the local data is not found, the loader will return various error results.  The error type is defined by your code.
 * <p/>
 * Created by kgalligan on 7/29/14.
 * TODO: Need error delivery
 */
public abstract class AbstractDoubleTapLoader<D, E> extends AbstractDataLoader<DoubleTapResult<D, E>>
{
    private BsyncTaskManager bsyncTaskManager;
    private boolean remoteCalled;
    private boolean remoteReturned;
    private E remoteError;

    public AbstractDoubleTapLoader(Context context)
    {
        super(context);
        bsyncTaskManager = new BsyncTaskManager(null);
        bsyncTaskManager.register(this);
    }

    class RemoteDataTask extends BsyncTask<AbstractDoubleTapLoader<D, E>>
    {
        @Override
        protected void doInBackground(Context context) throws Exception
        {
            E theError = findRemoteContent();
            synchronized (AbstractDoubleTapLoader.this)
            {
                remoteError = theError;
                remoteReturned = true;
            }
        }

        @Override
        protected void onPostExecute(AbstractDoubleTapLoader<D, E> host)
        {
            host.onContentChanged();
        }

        @Override
        public boolean handleError(Exception e)
        {
            return false;
        }
    }


    @Override
    protected void onReleaseResources(DoubleTapResult<D, E> data)
    {
        super.onReleaseResources(data);
        bsyncTaskManager.unregister();
    }

    @Override
    protected final DoubleTapResult<D, E> findContent() throws Exception
    {
        if (!remoteCalled)
        {
            remoteCalled = true;
            bsyncTaskManager.post(getContext(), new RemoteDataTask());
        }

        D localContent = findLocalContent();
        boolean localFound = localContent != null;
        boolean remoteDone;
        E e;

        synchronized (this)
        {
            remoteDone = remoteReturned;
            e = remoteError;
        }

        boolean remoteError = e != null;

        if (localFound)
        {
            return new DoubleTapResult<D, E>(DoubleTapResult.Status.Data, localContent, null);
        }
        else
        {
            if (!remoteDone)
            {
                return new DoubleTapResult<D, E>(DoubleTapResult.Status.Waiting, null, null);
            }
            else
            {
                if (remoteError)
                    return new DoubleTapResult<D, E>(DoubleTapResult.Status.Error, null, e);
                else
                    return new DoubleTapResult<D, E>(DoubleTapResult.Status.NoData, null, null);
            }
        }
    }

    protected abstract D findLocalContent() throws Exception;

    protected abstract E findRemoteContent() throws Exception;

    @Override
    protected boolean handleError(Exception e)
    {
        return false;
    }
}
