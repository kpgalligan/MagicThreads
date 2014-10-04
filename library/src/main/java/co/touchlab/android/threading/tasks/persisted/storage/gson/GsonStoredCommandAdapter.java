package co.touchlab.android.threading.tasks.persisted.storage.gson;

import co.touchlab.android.threading.tasks.persisted.Command;
import co.touchlab.android.threading.tasks.persisted.SuperbusProcessException;
import co.touchlab.android.threading.tasks.persisted.storage.StoredCommandAdapter;
import com.google.gson.Gson;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 10/13/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class GsonStoredCommandAdapter implements StoredCommandAdapter
{
    @Override
    public Command inflateCommand(String data, String className) throws SuperbusProcessException, ClassNotFoundException
    {
        try
        {
            Object returnedCommand = new Gson().fromJson(data, Class.forName(className));
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

    @Override
    public String storeCommand(Command command) throws SuperbusProcessException
    {
        try
        {
            return new Gson().toJson(command, command.getClass());
        }
        catch (Exception e)
        {
            throw new SuperbusProcessException(e);
        }
    }
}