package co.touchlab.android.threading.tasks.persisted;

import android.content.ContentValues;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.touchlab.android.threading.tasks.persisted.storage.StoredCommandAdapter;
import co.touchlab.android.threading.tasks.persisted.storage.gson.GsonStoredCommandAdapter;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.CursorIntf;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.SQLiteDatabaseFactory;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.SQLiteDatabaseIntf;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 10/13/12
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommandPersistenceProvider implements PersistenceProvider
{
    public static final String TABLE_NAME = "__SQL_PERS_PROV";
    public static final String COLUMNS = "id INTEGER PRIMARY KEY AUTOINCREMENT, type VARCHAR, commandData VARCHAR";
    public static final String[] COLUMN_LIST = {"id", "type", "commandData"};

    private Set<Class> checkedCommandClasses = new HashSet<Class>();
    private BusLog log;
    private SQLiteDatabaseFactory databaseFactory;
    private StoredCommandAdapter storedCommandAdapter;

    public CommandPersistenceProvider(SQLiteDatabaseFactory databaseFactory)
    {
        this(databaseFactory, null);
    }

    public CommandPersistenceProvider(SQLiteDatabaseFactory databaseFactory, BusLog log)
    {
        this(databaseFactory, new GsonStoredCommandAdapter(), log);
    }

    public CommandPersistenceProvider(SQLiteDatabaseFactory databaseFactory, StoredCommandAdapter storedCommandAdapter, BusLog log)
    {
        this.databaseFactory = databaseFactory;
        this.storedCommandAdapter = storedCommandAdapter;
        this.log = log == null ? new BusLogImpl() : log;
    }

    @Override
    public void removeCommand(PersistedTask persistedTask) throws SuperbusProcessException
    {
        try
        {
            int removedCount = databaseFactory.getDatabase().delete(TABLE_NAME, "id = ?", new String[]{persistedTask.getId().toString()});
            if (removedCount != 1)
                log.e(PersistedTaskQueue.TAG, "Deleted count != 1, was " + removedCount);
        }
        catch (Exception e)
        {
            if (e instanceof SuperbusProcessException)
                throw (SuperbusProcessException) e;
            else
                throw new SuperbusProcessException(e);
        }
    }

    @Override
    public void saveCommand(PersistedTask persistedTask) throws SuperbusProcessException
    {
        //Sanity check. StoredCommand classes need a no-arg constructor
        checkNoArg(persistedTask);

        try
        {
            ContentValues values = prepCommandSave(persistedTask);

            if (persistedTask.getId() == null)
            {
                long newRowId = databaseFactory.getDatabase().insertOrThrow(
                        TABLE_NAME, "type", values
                );

                persistedTask.setId(newRowId);
            }
            else
            {
                databaseFactory.getDatabase().update(TABLE_NAME, values, "id = ?", new String[]{persistedTask.getId().toString()});
            }
        }
        catch (SuperbusProcessException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new SuperbusProcessException(e);
        }
    }

    @Override
    public void saveCommandBatch(Collection<PersistedTask> persistedTasks) throws SuperbusProcessException
    {
        SQLiteDatabaseIntf database = databaseFactory.getDatabase();
        try
        {
            database.beginTransaction();
            for (PersistedTask persistedTask : persistedTasks)
            {
                saveCommand(persistedTask);
            }
            database.setTransactionSuccessful();
        }
        finally
        {
            database.endTransaction();
        }
    }

    @Override
    public Collection<PersistedTask> loadPersistedCommands() throws SuperbusProcessException
    {
        try
        {
            SQLiteDatabaseIntf db = databaseFactory.getDatabase();

            //Run query in a transaction to block changes while loading.  Probably not critical, but good for consistency
            db.beginTransaction();

            try
            {
                CursorIntf cursor = db.query(TABLE_NAME, COLUMN_LIST);

                List<PersistedTask> persistedTasks = null;
                try
                {
                    persistedTasks = new ArrayList<PersistedTask>();

                    while (cursor.moveToNext())
                    {
                        PersistedTask persistedTask = loadFromCursor(cursor);
                        if (persistedTask != null)
                            persistedTasks.add(persistedTask);
                    }
                }
                finally
                {
                    cursor.close();
                }

                db.setTransactionSuccessful();

                return persistedTasks;
            }
            finally
            {
                db.endTransaction();
            }
        }
        catch (Exception e)
        {
            throw new SuperbusProcessException(e);
        }
    }

    @Override
    public void clearPersistedCommands() throws SuperbusProcessException
    {
        try
        {
            databaseFactory.getDatabase().delete(TABLE_NAME, null, null);
        }
        catch (Exception e)
        {
            if (e instanceof SuperbusProcessException)
                throw (SuperbusProcessException) e;
            else
                throw new SuperbusProcessException(e);
        }
    }

    @Override
    public void runInTransaction(Runnable r)
    {
        SQLiteDatabaseIntf db = databaseFactory.getDatabase();
        db.beginTransaction();

        try
        {
            r.run();
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }
    }

    private ContentValues prepCommandSave(PersistedTask persistedTask) throws SuperbusProcessException
    {
        String commandData = storedCommandAdapter.storeCommand(persistedTask);

        ContentValues values = new ContentValues();

        values.put("type", persistedTask.getClass().getName());
        values.put("commandData", commandData);
        return values;
    }

    protected void checkNoArg(PersistedTask persistedTask) throws SuperbusProcessException
    {
        Class<? extends PersistedTask> commandClass = persistedTask.getClass();

        if (checkedCommandClasses.contains(commandClass))
            return;

        boolean isNoArg = false;

        Constructor<?>[] constructors = commandClass.getConstructors();

        for (Constructor<?> constructor : constructors)
        {
            if (constructor.getParameterTypes().length == 0)
            {
                isNoArg = true;
                break;
            }
        }

        if (!isNoArg)
            throw new SuperbusProcessException("All StoredCommand classes must have a no-arg constructor");

        checkedCommandClasses.add(commandClass);
    }

    private PersistedTask loadFromCursor(CursorIntf c) throws SuperbusProcessException
    {
        try
        {
            long id = c.getLong(0);
            String type = c.getString(1);
            String commandData = c.getString(2);

            PersistedTask storedPersistedTask = storedCommandAdapter.inflateCommand(commandData, type);

            storedPersistedTask.setId(id);

            return storedPersistedTask;
        }
        catch (Exception e)
        {
            if (e instanceof ClassNotFoundException)
            {
                log.e(PersistedTaskQueue.TAG, "Class cast on load. Nothing to do here. Be more careful.", e);
                return null;
            }
            else if (e instanceof SuperbusProcessException)
                throw (SuperbusProcessException) e;
            else
                throw new SuperbusProcessException(e);
        }
    }

    public static void createTables(SQLiteDatabaseIntf database)
    {
        database.execSQL("create table " + TABLE_NAME + " (" + COLUMNS + ")");
    }

    public static void dropTables(SQLiteDatabaseIntf database) throws SuperbusProcessException
    {
        database.execSQL("drop table " + TABLE_NAME);
    }
}
