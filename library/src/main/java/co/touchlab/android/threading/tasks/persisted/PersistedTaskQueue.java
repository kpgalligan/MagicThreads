package co.touchlab.android.threading.tasks.persisted;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import co.touchlab.android.threading.errorcontrol.SoftException;
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
    private final PollRunnable pollRunnable = new PollRunnable();
    private final PersistAllPendingRunnable persistAllPendingRunnable = new PersistAllPendingRunnable();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(r);
        }
    });
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

        pendingTasks.add(task);

        handler.removeCallbacks(persistAllPendingRunnable);
        handler.post(persistAllPendingRunnable);
    }

    //Run once, at the end of a batch
    private class PersistAllPendingRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();
            List<Command> copyPendingTasks = new ArrayList<Command>(pendingTasks);
            pendingTasks.clear();
            runInBackground(new PersistTasksRunnable(copyPendingTasks));
            for (Command copyPendingTask : copyPendingTasks)
            {
                commandQueue.offer(copyPendingTask);
            }
        }
    }

    private boolean checkHasDuplicate(Command c)
    {
        UiThreadContext.assertUiThread();

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
            resetPollRunnable();
            log.d(TAG, "PersistTasksRunnable - end - " + (System.currentTimeMillis() - start));
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
                c.setTransientExceptionCount(c.getTransientExceptionCount() + 1);
                provider.saveCommand(c);
            }

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
            UiThreadContext.assertUiThread();

            currentTask = null;

            switch (commandResult)
            {
                case Success:
                    c.onComplete(appContext);
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

        command.run(appContext);

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
