package co.touchlab.android.threading.tasks.persisted.storage;

import co.touchlab.android.threading.tasks.persisted.PersistedTask;
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
    PersistedTask inflateCommand(String data, String className) throws SuperbusProcessException, ClassNotFoundException;

    String storeCommand(PersistedTask persistedTask) throws SuperbusProcessException;
}
