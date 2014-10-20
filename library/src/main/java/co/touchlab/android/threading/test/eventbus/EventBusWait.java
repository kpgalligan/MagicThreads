package co.touchlab.android.threading.test.eventbus;

import de.greenrobot.event.EventBus;

/**
 * Created by kgalligan on 10/12/14.
 */
public class EventBusWait
{
    final Object mon = new Object();

    EventBus eventBus;

    boolean done;
    boolean set;

    public EventBusWait(EventBus eventBus)
    {
        this.eventBus = eventBus;
        eventBus.register(this);

    }

    public void waitEvent()
    {
        synchronized (mon)
        {
            if(done)
                return;

            set = true;

            try
            {
                mon.wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void notifyEvent()
    {
        synchronized (mon)
        {
            done = true;
            if(set)
                mon.notify();
        }
    }
}
