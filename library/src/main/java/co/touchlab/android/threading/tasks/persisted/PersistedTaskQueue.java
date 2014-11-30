package co.touchlab.android.threading.tasks.persisted;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import co.touchlab.android.threading.errorcontrol.SoftException;
import co.touchlab.android.threading.tasks.Task;
import co.touchlab.android.threading.utils.UiThreadContext;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by kgalligan on 9/28/14.
 */
public class PersistedTaskQueue
{
    public static final String TAG = PersistedTaskQueue.class.getSimpleName();

    private final Handler handler;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(r);
        }
    });

    private Queue<Command> addingTasks = new LinkedList<Command>();
    private Queue<Command> pendingTasks = new LinkedList<Command>();

    private Queue<Command> commandQueue = new PriorityQueue<Command>();

    private Command currentTask;

    private PersistenceProvider provider;
    private Application appContext;
    private CommandPurgePolicy commandPurgePolicy;
    private BusLog log;

    public PersistedTaskQueue(Application appContext, PersistedTaskQueueConfig config)
    {
        handler = new QueueHandler(Looper.getMainLooper());
        this.appContext = appContext;
        provider = config.getPersistenceProvider();
        commandPurgePolicy = config.commandPurgePolicy;
        log = config.getLog();
        runInBackground(new LoadAllRunnable());
    }

    private class QueueHandler extends Handler
    {
        static final int CALL_EXECUTE = 0;
        static final int POLL_TASK = 1;
        static final int POST_EXE = 2;
        static final int PERSIST_ALL_ADDING = 3;
        static final int TRIGGER_PENDING = 4;

        private QueueHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case CALL_EXECUTE:
                    callExecute((Command) msg.obj);
                    break;
                case POLL_TASK:
                    if (currentTask != null)
                        return;

                    Command command = commandQueue.poll();
                    if (command != null)
                    {
                        currentTask = command;
                        runInBackground(new ExeTask(command));
                    }
                    break;
                case POST_EXE:
                    try
                    {
                        FinishTaskContainer container = (FinishTaskContainer)msg.obj;

                        currentTask = null;

                        switch (container.commandResult)
                        {
                            case Success:
                                container.c.onComplete(appContext);
                                resetPollRunnable();
                                break;

                            case Transient:
                                logTransientException(container.c, container.cause);
                                commandQueue.add(container.c);
                                break;

                            case Permanent:
                                logPermanentException(container.c, container.cause);
                                resetPollRunnable();
                                break;

                            default:
                                throw new SuperbusProcessException("Unknown status");
                        }
                    }
                    finally
                    {
                        resetPollRunnable();
                    }
                    break;

                case PERSIST_ALL_ADDING:
                    if(!addingTasks.isEmpty())
                    {
                        List<Command> copyPendingTasks = new ArrayList<Command>(addingTasks);
                        pendingTasks.addAll(addingTasks);
                        addingTasks.clear();
                        runInBackground(new PersistTasksRunnable(copyPendingTasks));
                    }
                    break;

                case TRIGGER_PENDING:
                    List<Command> copyPersisted = (List<Command>)msg.obj;
                    pendingTasks.removeAll(copyPersisted);
                    for (Command copyPendingTask : copyPersisted)
                    {
                        commandQueue.offer(copyPendingTask);
                    }
                    resetPollRunnable();
                    break;
            }
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
            handler.sendMessage(handler.obtainMessage(QueueHandler.CALL_EXECUTE, task));
        }
    }

    /**
     * Added for testing, but you can use this as long as you're careful.  Somebody will blow up their app by editing the commands, but
     * I told you not to, so that's your problem.
     *
     * @return
     */
    public PersistedTaskQueueState copyState()
    {
        UiThreadContext.assertUiThread();

        PriorityQueue<Command> commands = new PriorityQueue<Command>(commandQueue);
        List<Command> commandList = new ArrayList<Command>();
        while (!commands.isEmpty())
        {
            commandList.add(commands.poll());
        }
        return new PersistedTaskQueueState(new ArrayList<Command>(pendingTasks), commandList, currentTask);
    }

    private void callExecute(Command task)
    {
        UiThreadContext.assertUiThread();

        boolean duplicate = checkHasDuplicate(task);

        if (duplicate)
            return;

        addingTasks.add(task);

        handler.removeMessages(QueueHandler.PERSIST_ALL_ADDING);
        handler.sendMessage(handler.obtainMessage(QueueHandler.PERSIST_ALL_ADDING));
    }

    private boolean checkHasDuplicate(Command c)
    {
        UiThreadContext.assertUiThread();

        boolean duplicate = checkCollectionHasDuplicate(c, addingTasks);

        if(!duplicate)
            duplicate = checkCollectionHasDuplicate(c, pendingTasks);

        if(!duplicate)
            duplicate = checkCollectionHasDuplicate(c, commandQueue);

        return duplicate;
    }

    private boolean checkCollectionHasDuplicate(Command c, Collection<Command> collection)
    {
        for (Command command : collection)
        {
            if (c.same(command))
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
                    commandQueue.addAll(commands);
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
            handler.sendMessage(handler.obtainMessage(QueueHandler.TRIGGER_PENDING, tasks));

            log.d(TAG, "PersistTasksRunnable - end - " + (System.currentTimeMillis() - start));
        }
    }

    private void resetPollRunnable()
    {
        handler.removeMessages(QueueHandler.POLL_TASK);
        handler.sendMessage(handler.obtainMessage(QueueHandler.POLL_TASK));
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

        command.run(appContext);

        logCommandVerbose(command, "callComand-finish");
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

        for (Command task : commandQueue)
        {
            queueQuery.query(task);
        }

        if (currentTask != null)
            queueQuery.query(currentTask);
    }

    public interface QueueQuery
    {
        void query(Command task);
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
        c.onTransientError(appContext, pe);
    }

    private void logPermanentException(Command c, Throwable e)
    {
        log.e(TAG, null, e);

        c.onPermanentError(appContext, e);
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
                    if(e instanceof RuntimeException)
                    {
                        throw (RuntimeException)e;
                    }
                    else if(e instanceof Error)//TODO: Intellij says this is always true, but I think its wrong...
                    {
                        throw (Error)e;
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
        List<Command> pending;
        List<Command> queued;
        Command currentTask;

        public PersistedTaskQueueState(List<Command> pending, List<Command> queued, Command currentTask)
        {
            this.pending = pending;
            this.queued = queued;
            this.currentTask = currentTask;
        }

        public List<Command> getPending()
        {
            return pending;
        }

        public List<Command> getQueued()
        {
            return queued;
        }

        public Command getCurrentTask()
        {
            return currentTask;
        }
    }
}
