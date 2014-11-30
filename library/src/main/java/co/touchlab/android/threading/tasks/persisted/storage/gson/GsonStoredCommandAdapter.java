package co.touchlab.android.threading.tasks.persisted.storage.gson;

import com.google.gson.Gson;

import co.touchlab.android.threading.tasks.persisted.Command;
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
    public Command inflateCommand(String data, String className) throws SuperbusProcessException, ClassNotFoundException
    {
        try
        {
            Gson gson = gsonForThread();
            Object returnedCommand = gson.fromJson(data, Class.forName(className));
            return (Command) returnedCommand;
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
    public String storeCommand(Command command) throws SuperbusProcessException
    {
        try
        {
            return gsonForThread().toJson(command, command.getClass());
        }
        catch (Exception e)
        {
            throw new SuperbusProcessException(e);
        }
    }
}
