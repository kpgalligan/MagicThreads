package co.touchlab.android.threading.loaders.networked;

import android.content.Context;
import co.touchlab.android.threading.loaders.AbstractDataLoader;
import co.touchlab.android.threading.tasks.BsyncTask;
import co.touchlab.android.threading.tasks.BsyncTaskManager;

/**
 * Created by kgalligan on 7/29/14.
 */
public abstract class AbstractDoubleTapLoader<D, E> extends AbstractDataLoader<DoubleTapResult<D, E>>
{
    private BsyncTaskManager bsyncTaskManager;

    public AbstractDoubleTapLoader(Context context)
    {
        super(context);
        bsyncTaskManager = new BsyncTaskManager(null);
        bsyncTaskManager.register(null);
    }

    class RemoteDataTask extends BsyncTask<AbstractDoubleTapLoader<D, E>>
    {
        @Override
        protected void doInBackground(Context context) throws Exception
        {
            //TODO: Route error info
            findRemoteContent();
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
        D localContent = findLocalContent();
        bsyncTaskManager.post(getContext(), new RemoteDataTask());
        return DoubleTapResult.result(localContent);
    }

    protected abstract D findLocalContent() throws Exception;

    protected abstract DoubleTapResult<D, E> findRemoteContent() throws Exception;

    @Override
    protected boolean handleError(Exception e)
    {
        return false;
    }
}
