package co.touchlab.android.threading.utils;

import android.os.Looper;

/**
 * Checks current thread, and throws a RuntimeException if you're not where you're supposed to
 * be.
 * <p/>
 * Created by kgalligan on 7/6/14.
 */
public class UiThreadContext
{
    /**
     * Checks if you're in the UI thread.
     */
    public static void assertUiThread()
    {
        if(! isInUiThread())
        {
            throw new RuntimeException("This call must be in UI thread");
        }
    }

    public static boolean isInUiThread()
    {
        Thread uiThread = Looper.getMainLooper().getThread();
        Thread currentThread = Thread.currentThread();

        return uiThread == currentThread;
    }

    /**
     * Checks if you're not in the UI thread.
     */
    public static void assertBackgroundThread()
    {
        if(isInUiThread())
        {
            throw new RuntimeException("This call must be in background thread");
        }
    }
}