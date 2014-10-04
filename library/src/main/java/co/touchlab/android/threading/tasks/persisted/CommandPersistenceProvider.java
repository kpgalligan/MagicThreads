package co.touchlab.android.threading.tasks.persisted;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import co.touchlab.android.threading.tasks.persisted.storage.StoredCommandAdapter;
import co.touchlab.android.threading.tasks.persisted.storage.gson.GsonStoredCommandAdapter;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.CursorIntf;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.SQLiteDatabaseFactory;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.SQLiteDatabaseIntf;
import co.touchlab.android.threading.utils.UiThreadContext;

import java.lang.reflect.Constructor;
import java.util.*;

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
    public void removeCommand(Command command) throws SuperbusProcessException
    {
        try
        {
            int removedCount = databaseFactory.getDatabase().delete(TABLE_NAME, "id = ?", new String[]{command.getId().toString()});
            if(removedCount != 1)
                log.e(PersistedTaskQueueActual.TAG, "Deleted count != 1, was "+ removedCount);
        }
        catch (Exception e)
        {
            if(e instanceof SuperbusProcessException)
                throw (SuperbusProcessException)e;
            else
                throw new SuperbusProcessException(e);
        }
    }

    @Override
    public void saveCommand(Command command) throws SuperbusProcessException
    {
        //Sanity check. StoredCommand classes need a no-arg constructor
        checkNoArg(command);

        try
        {
            ContentValues values = prepCommandSave(command);

            long newRowId = databaseFactory.getDatabase().insertOrThrow(
                    TABLE_NAME, "type", values
            );

            command.setId(newRowId);
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
    public Collection<Command> loadPersistedCommands()throws SuperbusProcessException
    {
        try
        {
            SQLiteDatabaseIntf db = databaseFactory.getDatabase();

            //Run query in a transaction to block changes while loading.  Probably not critical, but good for consistency
            db.beginTransaction();

            try
            {
                CursorIntf cursor = db.query(TABLE_NAME, COLUMN_LIST);

                List<Command> commands = null;
                try
                {
                    commands = new ArrayList<Command>();

                    while (cursor.moveToNext())
                    {
                        Command command = loadFromCursor(cursor);
                        if(command != null)
                            commands.add(command);
                    }
                }
                finally
                {
                    cursor.close();
                }

                db.setTransactionSuccessful();

                return commands;
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

    private ContentValues prepCommandSave(Command command) throws SuperbusProcessException
    {
        String commandData = storedCommandAdapter.storeCommand(command);

        ContentValues values = new ContentValues();

        values.put("type", command.getClass().getName());
        values.put("commandData", commandData);
        return values;
    }

    protected void checkNoArg(Command command)throws SuperbusProcessException
    {
        Class<? extends Command> commandClass = command.getClass();

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

    private Command loadFromCursor(CursorIntf c) throws SuperbusProcessException
    {
        try
        {
            long id = c.getLong(0);
            String type = c.getString(1);
            String commandData = c.getString(2);

            Command storedCommand = storedCommandAdapter.inflateCommand(commandData, type);

            storedCommand.setId(id);

            return storedCommand;
        }
        catch (Exception e)
        {
            if(e instanceof ClassNotFoundException)
            {
                log.e(PersistedTaskQueueActual.TAG, "Class cast on load. Nothing to do here. Be more careful.", e);
                return null;
            }
            else if(e instanceof SuperbusProcessException)
                throw (SuperbusProcessException)e;
            else
                throw new SuperbusProcessException(e);
        }
    }

    public static void createTables(SQLiteDatabaseIntf database)
    {
        database.execSQL("create table "+ TABLE_NAME +" ("+ COLUMNS +")");
    }

    public static void dropTables(SQLiteDatabaseIntf database) throws SuperbusProcessException
    {
        database.execSQL("drop table "+ TABLE_NAME);
    }
}
