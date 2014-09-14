package co.touchlab.magicthreadsdemo.loaders;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import co.touchlab.android.threading.loaders.networked.AbstractDoubleTapEventBusLoader;
import co.touchlab.magicthreadsdemo.R;
import co.touchlab.magicthreadsdemo.data.AppPrefs;
import co.touchlab.magicthreadsdemo.data.DataHelper;
import co.touchlab.magicthreadsdemo.data.DemoSimpleEvent;
import co.touchlab.magicthreadsdemo.data.EventRequest;
import co.touchlab.magicthreadsdemo.tasks.NullTask;

/**
 * Created by kgalligan on 9/13/14.
 */
public class EventDoubleTapLoader extends AbstractDoubleTapEventBusLoader<List<DemoSimpleEvent>, Integer>
{
    public EventDoubleTapLoader(Context context)
    {
        super(context);
    }

    @Override
    protected List<DemoSimpleEvent> findLocalContent() throws Exception
    {
        Type listType = new TypeToken<ArrayList<DemoSimpleEvent>>() {}.getType();

        String cachedData = AppPrefs.getInstance(getContext()).getCachedData();
        return cachedData == null ? null : (List<DemoSimpleEvent>) new Gson().fromJson(cachedData, listType);
    }

    @Override
    protected Integer findRemoteContent() throws Exception
    {
        //Artificial delay
        Thread.sleep(3000);

        List<DemoSimpleEvent> demoSimpleEvents = DataHelper.makeRequestAdapterBuilder(getContext()).build().create(EventRequest.class).demoAppAllEvents((long) getContext().getResources().getInteger(R.integer.conventionId));
        AppPrefs.getInstance(getContext()).setCachedData(new Gson().toJson(demoSimpleEvents));

        //Return an in referring to an error string. Error type is defined in the generic declaration. Can be whatever.
        return null;
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
