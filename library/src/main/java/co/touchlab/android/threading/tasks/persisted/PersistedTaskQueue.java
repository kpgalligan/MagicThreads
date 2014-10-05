package co.touchlab.android.threading.tasks.persisted;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import co.touchlab.android.threading.errorcontrol.SoftException;
import co.touchlab.android.threading.utils.UiThreadContext;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by kgalligan on 9/28/14.
 */
public class PersistedTaskQueue
{
    public static final String TAG = PersistedTaskQueue.class.getSimpleName();

    private final Handler handler;
    private final PollRunnable pollRunnable = new PollRunnable();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Queue<Command> pendingTasks = new LinkedList<Command>();

    private PriorityQueue<Command> commandQueue = new PriorityQueue<Command>();

    private Command currentTask;

    private PersistenceProvider provider;
    private Application appContext;
    private CommandPurgePolicy commandPurgePolicy;
    private BusLog log;

    public PersistedTaskQueue(Application appContext, PersistedTaskQueueConfig config)
    {
        UiThreadContext.assertUiThread();

        handler = new Handler(Looper.getMainLooper());
        this.appContext = appContext;
        provider = config.getPersistenceProvider();
        commandPurgePolicy = config.commandPurgePolicy;
        log = config.getLog();
        runInBackground(new LoadAllRunnable());
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
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    callExecute(task);
                }
            });
        }
    }

    /**
     * For testing purposes only.  Don't call this.  Stops queue and returns state for us to check out in test cases.
     * Would need to make sure we get back into a runnable state for this to work properly as a useful method.  Multithreading is hard.
     *
     * @return
     */
    public PersistedTaskQueueState clearQueue()
    {
        UiThreadContext.assertUiThread();

        executorService.execute(new Runnable()
        {
            @Override
            public void run()
            {
                provider.clearPersistedCommands();
            }
        });
        executorService.shutdown();
        try
        {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            log.e(TAG, "Wait interrupted", e);
        }

        PersistedTaskQueueState state = new PersistedTaskQueueState(new ArrayList<Command>(pendingTasks), new ArrayList<Command>(commandQueue), currentTask);

        pendingTasks.clear();
        commandQueue.clear();
        currentTask = null;

        handler.removeCallbacksAndMessages(null);

        return state;
    }

    /**
     * Added for testing, but you can use this as long as you're careful.  Somebody will blow up their app by editing the commands, but
     * I told you not to, so that's your problem.
     *
     * @return
     */
    public PersistedTaskQueueState copyState()
    {
        return new PersistedTaskQueueState(new ArrayList<Command>(pendingTasks), new ArrayList<Command>(commandQueue), currentTask);
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

    private void callExecute(Command task)
    {
        UiThreadContext.assertUiThread();

        boolean duplicate = checkHasDuplicate(task);

        if (duplicate)
            return;

        pendingTasks.add(task);

        runInBackground(new PersistTaskRunnable(task));
    }

    private boolean checkHasDuplicate(Command c)
    {
        boolean duplicate = false;

        for (Command command : pendingTasks)
        {
            if (c.same(command))
            {
                duplicate = true;
                break;
            }
        }

        if (duplicate)
        {
            for (Command command : commandQueue)
            {
                if (c.same(command))
                {
                    duplicate = true;
                    break;
                }
            }
        }
        return duplicate;
    }

    private class LoadAllRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertBackgroundThread();

            Collection<Command> commands = provider.loadPersistedCommands();
            //This touches the queue in the background thread, but should always happen
            //before main thread ops.
            commandQueue.addAll(commands);

            resetPollRunnable();
        }
    }

    private class PersistTaskRunnable implements Runnable
    {
        private Command task;

        private PersistTaskRunnable(Command task)
        {
            this.task = task;
        }

        @Override
        public void run()
        {
            UiThreadContext.assertBackgroundThread();

            provider.saveCommand(task);
            handler.post(new FlipQueuesRunnable(task));
        }
    }

    private class FlipQueuesRunnable implements Runnable
    {
        private Command task;

        private FlipQueuesRunnable(Command task)
        {
            this.task = task;
        }

        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();

            pendingTasks.remove(task);
            commandQueue.add(task);
            resetPollRunnable();
        }
    }

    private void resetPollRunnable()
    {
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
    }

    public void restartQueue()
    {
        resetPollRunnable();
    }

    private class PollRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();

            if (currentTask != null)
                return;

            logQueueState();

            Command command = commandQueue.poll();
            if (command != null)
            {
                currentTask = command;
                runInBackground(new ExeTask(command));
            }
        }
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
                provider.saveCommand(c);
            }
            //TODO: increment transient count
            handler.post(new FinishTaskRunnable(c, commandResult, cause));

        }
    }

    private class FinishTaskRunnable implements Runnable
    {
        private Command c;
        private CommandResult commandResult;
        private Throwable cause;

        private FinishTaskRunnable(Command c, CommandResult commandResult, Throwable cause)
        {
            this.c = c;
            this.commandResult = commandResult;
            this.cause = cause;
        }

        @Override
        public void run()
        {
            currentTask = null;

            switch (commandResult)
            {
                case Success:
                    c.onSuccess(appContext);
                    resetPollRunnable();
                    break;

                case Transient:
                    logTransientException(c, cause);
                    commandQueue.add(c);
                    break;

                case Permanent:
                    logPermanentException(c, cause);
                    resetPollRunnable();
                    break;

                default:
                    throw new SuperbusProcessException("Unknown status");
            }
        }
    }

    private void callCommand(final Command command) throws Throwable
    {
        logCommandVerbose(command, "callCommand-start");

        command.callCommand(appContext);

        logCommandVerbose(command, "callComand-finish");
    }

    private class ThrowRunnable implements Runnable
    {
        private Throwable cause;

        private ThrowRunnable(Throwable cause)
        {
            this.cause = cause;
        }

        @Override
        public void run()
        {
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else if (cause instanceof Error)
                throw (Error) cause;
            else
                throw new RuntimeException(cause);
        }
    }

    /**
     * Query existing tasks.  Call on main thread only.
     *
     * @param queueQuery
     */
    public void query(QueueQuery queueQuery)
    {
        UiThreadContext.assertUiThread();

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

    private void runInBackground(Runnable r)
    {
        executorService.execute(r);
    }

}
