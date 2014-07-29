package co.touchlab.android.threading.eventbus;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.SubscriberExceptionEvent;

/**
 * Created by kgalligan on 7/28/14.
 */
public class EventBusExt
{
    private static EventBusExt instance = new EventBusExt();
    private EventBus eventBus;

    public static class ErrorListener
    {
        public void onEvent(SubscriberExceptionEvent exceptionEvent)
        {
            final Throwable throwable = exceptionEvent.throwable;

            //EventBus will just log this.  Would prefer to blow up.
            new Thread()
            {
                @Override
                public void run()
                {
                    if (throwable instanceof RuntimeException)
                        throw (RuntimeException) throwable;
                    else if (throwable instanceof Error)
                        throw (Error) throwable;
                    else
                        throw new RuntimeException(throwable);
                }
            }.start();
        }
    }

    public EventBusExt()
    {
        eventBus = new EventBus();
        eventBus.register(new ErrorListener());
    }

    public static EventBus getDefault()
    {
        return instance.eventBus;
    }
}
