package co.touchlab.android.threading.errorcontrol;

/**
 * Created by kgalligan on 8/1/14.
 */
public class NetworkException extends SoftException
{
    public NetworkException()
    {
    }

    public NetworkException(String message)
    {
        super(message);
    }

    public NetworkException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NetworkException(Throwable cause)
    {
        super(cause);
    }
}
