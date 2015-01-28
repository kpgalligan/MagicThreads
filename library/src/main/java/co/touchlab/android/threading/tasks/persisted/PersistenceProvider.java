package co.touchlab.android.threading.tasks.persisted;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/25/13
 * Time: 1:57 AM
 * To change this template use File | Settings | File Templates.
 */
public interface PersistenceProvider
{
    void removeCommand(PersistedTask persistedTask) throws SuperbusProcessException;

    void saveCommand(PersistedTask persistedTask) throws SuperbusProcessException;

    void saveCommandBatch(Collection<PersistedTask> persistedTasks) throws SuperbusProcessException;

    Collection<PersistedTask> loadPersistedCommands() throws SuperbusProcessException;

    void clearPersistedCommands() throws SuperbusProcessException;

    void runInTransaction(Runnable r);
}
