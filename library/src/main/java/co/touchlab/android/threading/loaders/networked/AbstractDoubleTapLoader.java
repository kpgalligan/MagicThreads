package co.touchlab.android.threading.loaders.networked;

import android.content.Context;
import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.loaders.AbstractDataLoader;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.android.threading.tasks.sticky.StickyTask;
import co.touchlab.android.threading.tasks.sticky.StickyTaskManager;

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
    private StickyTaskManager stickyTaskManager;
    private boolean remoteCalled;
    private boolean remoteReturned;
    private E remoteError;
    private final TaskQueue loaderQueue;

    public AbstractDoubleTapLoader(Context context)
    {
        super(context);
        stickyTaskManager = new StickyTaskManager(null);
        loaderQueue = TaskQueue.loadQueue(context, "LOADER_QUEUE");
        EventBusExt.getDefault().register(this);
    }

    class StickyRemoteDataTask extends StickyTask
    {
        protected StickyRemoteDataTask(StickyTaskManager taskManager)
        {
            super(taskManager);
        }

        @Override
        protected void run(Context context) throws Exception
        {
            E theError = findRemoteContent();
            synchronized (AbstractDoubleTapLoader.this)
            {
                remoteError = theError;
                remoteReturned = true;
            }
        }

        @Override
        protected void onComplete(Context context)
        {
            EventBusExt.getDefault().post(this);
        }

        @Override
        protected boolean handleError(Context context, Throwable e)
        {
            return false;
        }
    }

    @Override
    protected void onReleaseResources(DoubleTapResult<D, E> data)
    {
        super.onReleaseResources(data);
        EventBusExt.getDefault().unregister(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(StickyRemoteDataTask task)
    {
        if(stickyTaskManager.isTaskForMe(task))
        {
            onContentChanged();
        }
    }

    @Override
    protected final DoubleTapResult<D, E> findContent() throws Exception
    {
        if (!remoteCalled)
        {
            remoteCalled = true;
            StickyRemoteDataTask stickyRemoteDataTask = new StickyRemoteDataTask(stickyTaskManager);
            loaderQueue.execute(stickyRemoteDataTask);
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
