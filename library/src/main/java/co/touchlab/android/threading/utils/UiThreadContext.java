package co.touchlab.android.threading.utils;

import android.os.Looper;

/**
 * Created by kgalligan on 7/6/14.
 */
public class UiThreadContext
{
    public static void assertUiThread()
    {
        Thread uiThread = Looper.getMainLooper().getThread();
        Thread currentThread = Thread.currentThread();

        if(uiThread != currentThread)
            throw new RuntimeException("This call must be in UI thread");
    }

    public static void assertBackgroundThread()
    {
        Thread uiThread = null;
        Thread currentThread = null;
        try
        {
            uiThread = Looper.getMainLooper().getThread();
            currentThread = Thread.currentThread();
        }
        catch (Exception e)
        {
            //Probably in unit tests
            return;
        }

        if(uiThread == currentThread)
            throw new RuntimeException("This call must be in background thread");
    }
}