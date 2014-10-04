package co.touchlab.android.threading.tasks.persisted;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import co.touchlab.android.threading.errorcontrol.SoftException;
import co.touchlab.android.threading.utils.UiThreadContext;

import java.util.Collection;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kgalligan on 9/28/14.
 */
public class PersistedTaskQueueActual
{
    private final Handler handler;
    private final PollRunnable pollRunnable = new PollRunnable();
    private final PostExeRunnable postExeRunnable = new PostExeRunnable();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Queue<Command> pendingTasks = new LinkedList<Command>();

    private PriorityQueue<Command> commandQueue;

    private Command currentTask;

    public static final String TAG = PersistedTaskQueueActual.class.getSimpleName();
    private PersistenceProvider provider;
    private Application appContext;
    private PersistedTaskQueueConfig config;
    private BusLog log;

    public PersistedTaskQueueActual(Application appContext, PersistedTaskQueueConfig config)
    {
        UiThreadContext.assertUiThread();

        handler = new Handler(Looper.getMainLooper());
        this.appContext = appContext;
        this.config = config;
        provider = config.getPersistenceProvider();
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
        if(UiThreadContext.isInUiThread())
        {
            callExecute(task);
        }
        else
        {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callExecute(task);
                }
            });
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

        if(duplicate)
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
            commandQueue = new PriorityQueue<Command>(commands);
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

    private class PollRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();

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
                try
                {
                    callCommand(c);
                    cause = null;
                    commandResult = CommandResult.Success;
                }
                catch (SoftException e)
                {
                    cause = e;

                    boolean purge = config.commandPurgePolicy.purgeCommandOnTransientException(c, e);

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

                if(cause != null)
                    log.e(TAG, null, cause);

                //Deal with status
                switch (commandResult)
                {
                    case Success:
                        provider.removeCommand(c);
                        c.onSuccess(appContext);
                        break;

                    case Transient:
                        c.setTransientExceptionCount(c.getTransientExceptionCount() + 1);//TODO: This will never be persisted.  Could be an issue.
                        logTransientException(c, cause);
                        handler.post(new RepostCommandRunnable(c));
                        break;

                    case Permanent:
                        provider.removeCommand(c);
                        logPermanentException(c, cause);
                        break;

                    default:
                        throw new SuperbusProcessException("Unknown status");
                }
            }
            finally
            {
                handler.post(postExeRunnable);
            }
        }
    }

    private void callCommand(final Command command) throws Throwable
    {
        logCommandVerbose(command, "callCommand-start");

        command.callCommand(appContext);

        logCommandVerbose(command, "callComand-finish");
    }

    private class RepostCommandRunnable implements Runnable
    {
        Command command;

        private RepostCommandRunnable(Command command)
        {
            this.command = command;
        }

        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();
            commandQueue.add(command);
        }
    }

    private class PostExeRunnable implements Runnable
    {
        @Override
        public void run()
        {
            UiThreadContext.assertUiThread();

            try
            {
                if(currentTask != null)
                {
                    Command task = currentTask;
                    currentTask = null;
                    task.onSuccess(appContext);
                }
            }
            finally
            {
                resetPollRunnable();
            }
        }
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
            if(cause instanceof RuntimeException)
                throw (RuntimeException)cause;
            else if(cause instanceof Error)
                throw (Error)cause;
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

        if(currentTask != null)
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
