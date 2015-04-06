package co.touchlab.android.threading.tasks.helper;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Basic service to be run while tasks are running.  May keep process alive.
 *
 * To make a foreground service, extend this class, and return values for getForegroundId and getForegroundNotification
 * Created by kgalligan on 4/4/15.
 */
public class KeepAliveService extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Notification foregroundNotification = getForegroundNotification();
        if(foregroundNotification != null)
        {
            startForeground(getForegroundId(), foregroundNotification);
        }

        return START_STICKY;
    }

    protected int getForegroundId()
    {
        return 0;
    }

    protected Notification getForegroundNotification()
    {
        return null;
    }
}
