package co.touchlab.magicthreadsdemo.loaders;

import android.content.Context;

import java.util.List;

import co.touchlab.android.threading.loaders.AbstractEventBusLoader;
import co.touchlab.magicthreadsdemo.R;
import co.touchlab.magicthreadsdemo.data.DataHelper;
import co.touchlab.magicthreadsdemo.data.DemoSimpleEvent;
import co.touchlab.magicthreadsdemo.data.EventRequest;
import co.touchlab.magicthreadsdemo.tasks.NullTask;

/**
 * Created by kgalligan on 9/13/14.
 */
public class EventLoader extends AbstractEventBusLoader<List<DemoSimpleEvent>>
{
    public EventLoader(Context context)
    {
        super(context);
    }

    @Override
    protected List<DemoSimpleEvent> findContent() throws Exception
    {
        return DataHelper.makeRequestAdapterBuilder(getContext()).build().create(EventRequest.class).demoAppAllEvents((long)getContext().getResources().getInteger(R.integer.conventionId));
    }

    @Override
    protected boolean handleError(Exception e)
    {
        return false;
    }

    /**
     * This won't actually happen in the demo, but EventBus blows up
     * if you don't have an onEvent, so...
     * @param task
     */
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(NullTask task)
    {
        onContentChanged();
    }
}
