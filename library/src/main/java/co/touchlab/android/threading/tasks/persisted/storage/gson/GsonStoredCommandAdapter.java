package co.touchlab.android.threading.tasks.persisted.storage.gson;

import com.google.gson.Gson;

import co.touchlab.android.threading.tasks.persisted.PersistedTask;
import co.touchlab.android.threading.tasks.persisted.SuperbusProcessException;
import co.touchlab.android.threading.tasks.persisted.storage.StoredCommandAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 10/13/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class GsonStoredCommandAdapter implements StoredCommandAdapter
{
    ThreadLocal<Gson> gsonThreadLocal = new ThreadLocal<Gson>();

    @Override
    public PersistedTask inflateCommand(String data, String className) throws SuperbusProcessException, ClassNotFoundException
    {
        try
        {
            Gson gson = gsonForThread();
            Object returnedCommand = gson.fromJson(data, Class.forName(className));
            return (PersistedTask) returnedCommand;
        }
        catch (ClassNotFoundException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new SuperbusProcessException(e);
        }
    }

    private Gson gsonForThread()
    {
        Gson gson = gsonThreadLocal.get();
        if (gson == null)
        {
            gson = new Gson();
            gsonThreadLocal.set(gson);
        }
        return gson;
    }

    @Override
    public String storeCommand(PersistedTask persistedTask) throws SuperbusProcessException
    {
        try
        {
            return gsonForThread().toJson(persistedTask, persistedTask.getClass());
        }
        catch (Exception e)
        {
            throw new SuperbusProcessException(e);
        }
    }
}
