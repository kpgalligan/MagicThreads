package co.touchlab.android.threading.tasks.persisted;

import android.app.Notification;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/25/13
 * Time: 12:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class PersistedTaskQueueConfig
{
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

        public PersistedTaskQueueConfig build() throws ConfigException
        {
            if (config.log == null)
                config.log = new BusLogImpl();
            if (config.commandPurgePolicy == null)
                config.commandPurgePolicy = new TransientMethuselahCommandPurgePolicy();

            if (config.persistenceProvider == null)
                throw new ConfigException("Superbus needs a persistence provider");

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
}
