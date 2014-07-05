package co.touchlab.android.threading.loaders;

import android.content.Context;
import de.greenrobot.event.EventBus;

/**
 * Created by kgalligan on 7/5/14.
 */
public abstract class AbstractEventBusLoader<D> extends AbstractDataLoader<D>
{
    private EventBus eventBus;

    public AbstractEventBusLoader(Context context)
    {
        this(context, EventBus.getDefault());
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
