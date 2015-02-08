package co.touchlab.android.threading.tasks.persisted;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import co.touchlab.android.threading.tasks.persisted.storage.sqlite.ClearSQLiteDatabase;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.SQLiteDatabaseFactory;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.SQLiteDatabaseIntf;
import co.touchlab.android.threading.tasks.persisted.storage.sqlite.SimpleDatabaseHelper;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/25/13
 * Time: 12:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class PersistedTaskQueueConfig
{
    public static final String PERSISTED_QUEUE = "PERSISTED_QUEUE";
    List<SuperbusEventListener> eventListeners = new ArrayList<SuperbusEventListener>();
    BusLog log;
    CommandPurgePolicy commandPurgePolicy;
    PersistenceProvider persistenceProvider;

    public static class Builder
    {
        PersistedTaskQueueConfig config = new PersistedTaskQueueConfig();

        private void checkState() throws ConfigException
        {
            if (config == null)
                throw new ConfigException("build already called");
        }

        public Builder addEventListener(SuperbusEventListener eventListener) throws ConfigException
        {
            checkState();
            config.eventListeners.add(eventListener);
            return this;
        }

        public Builder setLog(BusLog l) throws ConfigException
        {
            checkState();
            config.log = l;
            return this;
        }

        public Builder setCommandPurgePolicy(CommandPurgePolicy p) throws ConfigException
        {
            checkState();
            config.commandPurgePolicy = p;
            return this;
        }

        public Builder setPersistenceProvider(PersistenceProvider p) throws ConfigException
        {
            checkState();
            config.persistenceProvider = p;
            return this;
        }

        public PersistedTaskQueueConfig build(Context context) throws ConfigException
        {
            if (config.log == null)
                config.log = new BusLogImpl();
            if (config.commandPurgePolicy == null)
                config.commandPurgePolicy = new TransientMethuselahCommandPurgePolicy();

            if (config.persistenceProvider == null)
                config.persistenceProvider = new CommandPersistenceProvider(
                        new LocalDatabaseFactory(context)
                );

            PersistedTaskQueueConfig retConfig = config;
            config = null;
            return retConfig;
        }
    }

    public List<SuperbusEventListener> getEventListeners()
    {
        return eventListeners;
    }

    public BusLog getLog()
    {
        return log;
    }

    public CommandPurgePolicy getCommandPurgePolicy()
    {
        return commandPurgePolicy;
    }

    public PersistenceProvider getPersistenceProvider()
    {
        return persistenceProvider;
    }

    private static final class LocalDatabaseFactory implements SQLiteDatabaseFactory
    {
        private ClearSQLiteDatabase sqLiteDatabase;

        private LocalDatabaseFactory(Context context)
        {
            sqLiteDatabase = new ClearSQLiteDatabase(SimpleDatabaseHelper.getInstance(context, PERSISTED_QUEUE).getWritableDatabase());
        }

        @Override
        public SQLiteDatabaseIntf getDatabase()
        {
            return sqLiteDatabase;
        }
    }

}
