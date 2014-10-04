package co.touchlab.android.threading.tasks.persisted;

/**
 * Created by kgalligan on 10/4/14.
 */
public class ConfigException extends Exception
{
    public ConfigException()
    {
        super();
    }

    public ConfigException(String detailMessage)
    {
        super(detailMessage);
    }

    public ConfigException(String detailMessage, Throwable throwable)
    {
        super(detailMessage, throwable);
    }

    public ConfigException(Throwable throwable)
    {
        super(throwable);
    }
}
