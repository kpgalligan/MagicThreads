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
    void removeCommand(Command command) throws SuperbusProcessException;
    void saveCommand(Command command)throws SuperbusProcessException;
    Collection<Command> loadPersistedCommands()throws SuperbusProcessException;
    void clearPersistedCommands()throws SuperbusProcessException;
    void runInTransaction(Runnable r);
}
