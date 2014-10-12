package co.touchlab.android.threading.tasks.persisted.storage;

import co.touchlab.android.threading.tasks.persisted.Command;
import co.touchlab.android.threading.tasks.persisted.SuperbusProcessException;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 10/13/12
 * Time: 5:15 AM
 * To change this template use File | Settings | File Templates.
 */
public interface StoredCommandAdapter
{
    Command inflateCommand(String data, String className) throws SuperbusProcessException, ClassNotFoundException;

    String storeCommand(Command command) throws SuperbusProcessException;
}
