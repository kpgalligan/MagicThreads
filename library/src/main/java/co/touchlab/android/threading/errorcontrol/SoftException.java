package co.touchlab.android.threading.errorcontrol;

/**
 * Created by kgalligan on 8/1/14.
 */
public class SoftException extends Exception
{
    public SoftException()
    {
    }

    public SoftException(String message)
    {
        super(message);
    }

    public SoftException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SoftException(Throwable cause)
    {
        super(cause);
    }
}
