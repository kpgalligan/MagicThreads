package co.touchlab.android.threading.tasks.persisted;

import android.content.Context;

/**
 * Created by kgalligan on 6/29/14.
 */
public abstract class CheckedCommand extends Command
{
    public abstract boolean handlePermanentError(Context context, Throwable exception);

    @Override
    public final void onPermanentError(Context context, Throwable exception)
    {
        boolean handled = handlePermanentError(context, exception);
        if(!handled)
            throw new SuperbusProcessException(exception);
    }
}
