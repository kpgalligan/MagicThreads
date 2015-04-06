package co.touchlab.android.threading.tasks.helper;
import android.content.Context;

import co.touchlab.android.threading.errorcontrol.NetworkException;
import co.touchlab.android.threading.errorcontrol.SoftException;
import co.touchlab.android.threading.tasks.persisted.PersistedTask;
import retrofit.RetrofitError;

/**
 * Created by kgalligan on 4/4/15.
 */
public abstract class RetrofitPersistedTask extends PersistedTask
{
    @Override
    protected final void run(Context context) throws SoftException, Throwable
    {
        try
        {
            runNetwork(context);
        }
        catch(RetrofitError e)
        {
            if(e.getKind() == RetrofitError.Kind.NETWORK)
            {
                throw new NetworkException(e);
            }
        }
    }

    protected  abstract void runNetwork(Context context);

    @Override
    protected boolean handleError(Context context, Throwable e)
    {
        return false;
    }
}
