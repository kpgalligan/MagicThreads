package co.touchlab.android.threading.tasks.persisted;

import android.app.Application;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import co.touchlab.android.threading.errorcontrol.SoftException;
import co.touchlab.android.threading.tasks.BaseTaskQueue;
import co.touchlab.android.threading.tasks.Task;
import co.touchlab.android.threading.utils.UiThreadContext;

/**
 * Created by kgalligan on 9/28/14.
 */
public class PersistedTaskQueue extends BaseTaskQueue
{
    public static final String TAG = PersistedTaskQueue.class.getSimpleName();

    static final int START_PERSISTING_TASK = 200;
    static final int PERSIST_ALL_ADDING = 300;
    static final int TRIGGER_PENDING = 400;

    private Queue<Command> addingTasks = new LinkedList<Command>();
    private Queue<Command> pendingTasks = new LinkedList<Command>();

    private PersistenceProvider provider;
    private CommandPurgePolicy commandPurgePolicy;
    private BusLog log;

    public PersistedTaskQueue(Application appContext, PersistedTaskQueueConfig config)
    {
        super(appContext);
        provider = config.getPersistenceProvider();
        commandPurgePolicy = config.commandPurgePolicy;
        log = config.getLog();
        runInBackground(new LoadAllRunnable());
    }

    @Override
    protected void otherOperations(Message msg)
    {
        switch (msg.what)
        {
            case START_PERSISTING_TASK:
                callExecute((Command) msg.obj);
                break;
            case PERSIST_ALL_ADDING:
                if (!addingTasks.isEmpty())
                {
                    List<Command> copyPendingTasks = new ArrayList<Command>(addingTasks);
                    pendingTasks.addAll(addingTasks);
                    addingTasks.clear();
                    runInBackground(new PersistTasksRunnable(copyPendingTasks));
                }
                break;
            case TRIGGER_PENDING:
                List<Command> copyPersisted = (List<Command>) msg.obj;
                pendingTasks.removeAll(copyPersisted);
                tasks.addAll(copyPersisted);
                resetPollRunnable();
                break;
        }
    }

    @Override
    protected void finishTask(Message msg, Task task)
    {
        FinishTaskContainer container = (FinishTaskContainer) msg.obj;
        boolean shouldReset = true;
        try
        {
            switch (container.commandResult)
            {
                case Success:
                    container.c.onComplete(application);
                    break;

                case Transient:
                    logTransientException(container.c, container.cause);
                    tasks.offer(container.c);
                    shouldReset = false;
                    break;

                case Permanent:
                    logPermanentException(container.c, container.cause);
                    break;

                default:
                    throw new SuperbusProcessException("Unknown status");
            }
        }
        finally
        {
            if (shouldReset)
                resetPollRunnable();
        }
    }

    /**
     * Puts a task on the queue.  Call on main thread only.
     *
     * @param task
     */
    public void execute(final Command task)
    {
        if (UiThreadContext.isInUiThread())
        {
            callExecute(task);
        }
        else
        {
            handler.sendMessage(handler.obtainMessage(START_PERSISTING_TASK, task));
        }
    }

    private void callExecute(Command task)
    {
        UiThreadContext.assertUiThread();

        boolean duplicate = checkHasDuplicate(task);

        if (duplicate)
            return;

        addingTasks.add(task);

        handler.removeMessages(PERSIST_ALL_ADDING);
        handler.sendMessage(handler.obtainMessage(PERSIST_ALL_ADDING));
    }

    /**
     * Added for testing, but you can use this as long as you're careful.  Somebody will blow up their app by editing the commands, but
     * I told you not to, so that's your problem.
     *
     * @return
     */
    public PersistedTaskQueueState copyPersistedState()
    {
        UiThreadContext.assertUiThread();

        TaskQueueState taskQueueState = copyState();

        return new PersistedTaskQueueState(new ArrayList<Command>(addingTasks), new ArrayList<Command>(pendingTasks), taskQueueState.getQueued(), taskQueueState.getCurrentTask());
    }


    private boolean checkHasDuplicate(Command c)
    {
        UiThreadContext.assertUiThread();

        boolean duplicate = checkCollectionHasDuplicate(c, addingTasks);

        if (!duplicate)
            duplicate = checkCollectionHasDuplicate(c, pendingTasks);

        if (!duplicate)
            duplicate = checkCollectionHasDuplicate(c, tasks);

        return duplicate;
    }

    private boolean checkCollectionHasDuplicate(Command c, Collection collection)
    {
        //Did this because generic collection type checking was nasty
        for (Object command : collection)
        {
            if (command instanceof Command && c.same((Command) command))
            {
                return true;
            }
        }

        return false;
    }

    private class LoadAllRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertBackgroundThread();

            final Collection<Command> commands = provider.loadPersistedCommands();

            //TODO: Checking 'same' on tasks added while loading from db may incorrectly return false and
            //add duplicate tasks.  To be complete, we should figure this out, but its a pretty minor issue
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    for (Command command : commands)
                    {
                        insertTask(command);
                    }
                }
            });

            resetPollRunnable();
        }
    }

    private class PersistTasksRunnable implements Runnable
    {
        private List<Command> tasks;

        private PersistTasksRunnable(List<Command> tasks)
        {
            this.tasks = tasks;
        }

        @Override
        public void run()
        {
            long start = System.currentTimeMillis();
            log.d(TAG, "PersistTasksRunnable - start");
            UiThreadContext.assertBackgroundThread();

            provider.saveCommandBatch(tasks);
            handler.sendMessage(handler.obtainMessage(TRIGGER_PENDING, tasks));

            log.d(TAG, "PersistTasksRunnable - end - " + (System.currentTimeMillis() - start));
        }
    }

    public void restartQueue()
    {
        resetPollRunnable();
    }

    private enum CommandResult
    {
        Success, Transient, Permanent
    }

    private class ExeTask implements Runnable
    {
        private Command c;

        private ExeTask(Command task)
        {
            this.c = task;
        }

        @Override
        public void run()
        {
            UiThreadContext.assertBackgroundThread();

            CommandResult commandResult;
            Throwable cause;

            try
            {
                callCommand(c);
                cause = null;
                commandResult = CommandResult.Success;
            }
            catch (SoftException e)
            {
                cause = e;

                boolean purge = commandPurgePolicy.purgeCommandOnTransientException(c, e);

                if (purge)
                {
                    log.w(TAG, "Purging command on TransientException: {" + c.logSummary() + "}");
                    commandResult = CommandResult.Permanent;
                }
                else
                {
                    commandResult = CommandResult.Transient;
                }
            }
            catch (Throwable e)
            {
                cause = e;
                commandResult = CommandResult.Permanent;
            }

            if (cause != null)
                log.e(TAG, null, cause);

            if (commandResult == CommandResult.Success || commandResult == CommandResult.Permanent)
            {
                provider.removeCommand(c);
            }
            else
            {
                c.setTransientExceptionCount(c.getTransientExceptionCount() + 1);
                provider.saveCommand(c);
            }

            handler.sendMessage(handler.obtainMessage(QueueHandler.POST_EXE, new FinishTaskContainer(c, commandResult, cause)));
        }
    }

    private static class FinishTaskContainer
    {
        private final Command c;
        private final CommandResult commandResult;
        private final Throwable cause;

        private FinishTaskContainer(Command c, CommandResult commandResult, Throwable cause)
        {
            this.c = c;
            this.commandResult = commandResult;
            this.cause = cause;
        }
    }

    private void callCommand(final Command command) throws Throwable
    {
        logCommandVerbose(command, "callCommand-start");

        command.run(application);

        logCommandVerbose(command, "callComand-finish");
    }

    @Override
    protected Queue<Task> createQueue()
    {
        return new PriorityQueue<Task>();
    }

    @Override
    protected void runTask(Task task)
    {
        runInBackground(new ExeTask((Command) task));
    }

    /**
     * Query existing tasks.  Call on main thread only.
     *
     * @param queueQuery
     */
    public void query(QueueQuery queueQuery)
    {
        UiThreadContext.assertUiThread();

        for (Command pendingTask : addingTasks)
        {
            queueQuery.query(pendingTask);
        }

        for (Command pendingTask : pendingTasks)
        {
            queueQuery.query(pendingTask);
        }

        super.query(queueQuery);
    }

    private void logQueueState()
    {
        //TODO
    }

    private void logCommandVerbose(Command command, String methodName)
    {
        try
        {
            log.v(TAG, methodName + ": " + command.getAdded() + " : " + command.logSummary());
        }
        catch (Exception e)
        {
            //Just in case...
        }
    }

    private void logTransientException(Command c, Throwable e)
    {
        log.e(TAG, null, e);
        SoftException pe = e instanceof SoftException ? (SoftException) e : new SoftException(e);
        c.onTransientError(application, pe);
    }

    private void logPermanentException(Command c, Throwable e)
    {
        log.e(TAG, null, e);

        c.onPermanentError(application, e);
    }

    private void runInBackground(final Runnable r)
    {
        executorService.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    r.run();
                }
                catch (Throwable e)
                {
                    if (e instanceof RuntimeException)
                    {
                        throw (RuntimeException) e;
                    }
                    else if (e instanceof Error)//TODO: Intellij says this is always true, but I think its wrong...
                    {
                        throw (Error) e;
                    }
                    else
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public static class PersistedTaskQueueState
    {
        List<Command> adding;
        List<Command> pending;
        List<Task> queued;
        Task currentTask;

        public PersistedTaskQueueState(List<Command> adding, List<Command> pending, List<Task> queued, Task currentTask)
        {
            this.adding = adding;
            this.pending = pending;
            this.queued = queued;
            this.currentTask = currentTask;
        }

        public List<Command> getAdding()
        {
            return adding;
        }

        public List<Command> getPending()
        {
            return pending;
        }

        public List<Task> getQueued()
        {
            return queued;
        }

        public Task getCurrentTask()
        {
            return currentTask;
        }
    }
}
