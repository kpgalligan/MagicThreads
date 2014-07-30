package co.touchlab.android.threading.loaders.networked;

/**
 * Created by kgalligan on 7/29/14.
 */
public class DoubleTapResult<D, E>
{
    private final D result;
    private final E error;

    public static <D, E> DoubleTapResult<D, E> result(D result)
    {
        return new DoubleTapResult<D, E>(result, null);
    }

    public static <D> DoubleTapResult<Void, D> error(D error)
    {
        return new DoubleTapResult<Void, D>(null, error);
    }

    public DoubleTapResult(D result, E error)
    {
        this.result = result;
        this.error = error;
    }

    public D getResult()
    {
        return result;
    }

    public E getError()
    {
        return error;
    }

    public boolean isError()
    {
        return error != null;
    }
}

