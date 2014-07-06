package co.touchlab.android.threading.tasks;

/**
 * Created by kgalligan on 7/5/14.
 */
public abstract class BsyncTask implements TaskQueue.Task
{
    protected int contextId;

    protected abstract void doInBackground()throws Exception;

    protected abstract void onPostExecute();

    @Override
    public final void run() throws Exception
    {
        doInBackground();
    }
}
