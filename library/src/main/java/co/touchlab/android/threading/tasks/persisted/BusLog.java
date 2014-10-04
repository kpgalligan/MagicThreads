package co.touchlab.android.threading.tasks.persisted;

/**
 * Created by kgalligan on 10/4/14.
 */
public interface BusLog
{
    int d(String tag, String msg);

    int d(String tag, String msg, Throwable tr);

    int e(String tag, String msg);

    int e(String tag, String msg, Throwable tr);

    String getStackTraceString(Throwable tr);

    int i(String tag, String msg);

    int i(String tag, String msg, Throwable tr);

    boolean isLoggable(String tag, int level);

    int println(int priority, String tag, String msg);

    int v(String tag, String msg);

    int v(String tag, String msg, Throwable tr);

    int w(String tag, Throwable tr);

    int w(String tag, String msg, Throwable tr);

    int w(String tag, String msg);

    void logSoftException(String tag, String message, Throwable tr);
}