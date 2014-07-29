package co.touchlab.android.threading.loaders;

import android.content.Context;
import co.touchlab.android.threading.eventbus.EventBusExt;
import de.greenrobot.event.EventBus;

/**
 * Loader using the EventBus for refresh triggers.  You'll need to implement onEvent methods
 * to actually capture events, and tell the loader to refresh.
 *
 * Created by kgalligan on 7/5/14.
 */
public abstract class AbstractEventBusLoader<D> extends AbstractDataLoader<D>
{
    private EventBus eventBus;

    public AbstractEventBusLoader(Context context)
    {
        this(context, EventBusExt.getDefault());
    }

    public AbstractEventBusLoader(Context context, EventBus eventBus)
    {
        super(context);
        this.eventBus = eventBus;
    }

    @Override
    protected void registerContentChangedObserver()
    {
        eventBus.register(this);
    }

    @Override
    protected void unregisterContentChangedObserver()
    {
        eventBus.unregister(this);
    }
}
