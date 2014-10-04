package co.touchlab.android.threading.tasks.persisted;

import android.content.Context;
import co.touchlab.android.threading.errorcontrol.SoftException;

import java.util.Map;

/**
 * Abstract class to wrap the operation you want to run.
 *
 * Commands are added to the PersistenceProvider, and called by the SuperbusService.
 * Commands will be processed in the order that they are added, unless a priority is set.
 * Commands with the same priority will be processed in the order they are added.
 *
 * Higher priority commands are processed BEFORE lower priority.  For example, priority 3 commands are processed after
 * priority 7 commands.
 *
 * Default priority is 10.
 *
 * Commands must include a no-arg constructor.  The bus service will fail without this.
 *
 * If your commands should be persisted, use StoredCommand instead, or some other custom
 * implementation.
 *
 * User: kgalligan
 * Date: 1/11/12
 * Time: 8:57 AM
 */
public abstract class Command implements Comparable<Command>
{
    public static final int MUCH_LOWER_PRIORITY = 1;
    public static final int LOWER_PRIORITY = 5;
    public static final int DEFAULT_PRIORITY = 10;
    public static final int HIGHER_PRIORITY = 15;
    public static final int MUCH_HIGHER_PRIORITY = 20;

    //Init with current time. Allow override by accessors
    private long lastUpdate = System.currentTimeMillis();

    private int priority = DEFAULT_PRIORITY;
    private long added = System.currentTimeMillis();
    private int transientExceptionCount = 0;

    private transient boolean commandRunning = false;

    public boolean isCommandRunning()
    {
        return commandRunning;
    }

    public void setCommandRunning(boolean commandRunning)
    {
        this.commandRunning = commandRunning;
    }

    /**
     * This is for your benefit.  Command info will be logged during various events.
     * @return String representation of the command.  Human readable.  Bus doesn't care what this is, but keep in mind common sense performance considerations.
     */
    public abstract String logSummary();

    /**
     * This is like Java equals, but could be more relaxed.  Used to test existing commands and exclude adding a new one.
     * The idea is if you have a generic command that only needs to run once, adding multiples will be excluded.  For example
     * say you have a "RefreshAccounts" command.  Running that multiple times would probably be wasteful.
     * To ALWAYS run the command, simply return false.
     *
     * @param command Command to test for "same-ness"
     * @return true if passed command is the same as this one.  False if not, or if the command should always run.
     */
    public abstract boolean same(Command command);

    /**
     * This is where your logic goes.  You must be very careful with how you handle error conditions.
     *
     * "Soft" issues should throw a TransientException.  This is almost always due to temporary network issues.  These
     * will cause the command to be put back on the queue, and processing to stop.  This preserves the command so its not
     * dropped.  HOWEVER!!!  If you throw a TransientException for something that won't resolve itself, by default
     * it will NEVER be removed.  You will build up commands forever, and (worse) stop further processing.
     *
     * To eventually quit command processing attempts, use a custom implementation of CommandPurgePolicy.
     *
     * Also, if processing is stopped due to a TransientException, that command and possibly others will remain
     * in the queue.  Some other event will be required to restart processing.  Either manual, or a network connection
     * listener.
     *
     * "Hard" issues should throw a PermanentException.  This will remove the command, and call onPermanentError.  Your
     * code will need to deal with the error.  Possibly show a notification to the user, revert DB changes, etc.  Processing
     * will continue on other commands, if they exist.
     *
     * Be aware, if latter commands depend on this one, they will probably trigger a cascade of errors.  Just FYI. Plan
     * accordingly.
     *
     * Any unchecked exception coming out of this call will be interpreted as a permanent issue, and also removed. This
     * includes instances of RuntimeException and Error.  In future releases, there may be a need to handle OutOfMemoryError
     * and similar issues in a different way.  If the command always triggers memory limits, it should be removed, but
     * if memory issues are triggered because the rest of the app was using too much memory temporarily, the command may
     * be removed unnecessarily.  Future releases should probably allow the SuperbusService to run in a separate process
     * to reduce the likelihood of this, but that is not possible today.
     *
     * @param context
     * @throws co.touchlab.android.threading.errorcontrol.SoftException
     * @throws java.lang.Throwable
     */
    public abstract void callCommand(Context context) throws SoftException, Throwable;

    /**
     * There was a transient problem with this command.  Its being put back on the queue.
     *
     * @param exception Exception that caused the removal
     */
    public void onTransientError(Context context, SoftException exception)
    {

    }

    /**
     * There was a permanent problem with this command and its being removed.  You will probably want to put some type of
     * notification or reversal code in this method.
     *
     * @param exception Exception that caused the removal
     */
    public void onPermanentError(Context context, Throwable exception)
    {

    }

    /**
     * Success!  Your command processed.
     */
    public void onSuccess(Context context)
    {

    }

    /**
     * To help with runtime coordination, apps can post messages to live commands.
     * The major use case.  A command that refreshes data from the server is looping through updates.
     * While that is happening, the user wants to update an entity.  Pass a message to cancel updates until the
     * remote update can be processed.  If commands set up properly, data won't be lost, but local data might
     * be temporarily lost, and to the user, it will seem like data has reverted.
     *
     * If using SQLite, here is the sequence.  It seems complex, but its a generic pattern.
     *
     * 1) refresh command starts.  While processing, UI resumes.
     * 2) User clicks "save", and the entity update process starts.
     * 3) The update code starts a transaction on the db.
     * 4) Pass a "stop refresh" message to the command queue.
     * 5) The refresh command is still loading remote data, but catches a flag to cancel.
     * 6) While still in the db transaction, update the local entity.
     * 7) Post the remote entity update command.
     * 8) Close the transaction.
     * 9) The refresh command, still in process, checks the cancel flag before updating local db. Cancel.
     * 10) If the refresh command interleaved updates, but still used transactions, as long as it checks the cancel
     * flag, there should be no point at which it overwrites the local modification.
     * 11) Optionally, have the refresh command repost itself.
     *
     * Make sure the local edit update has a higher priority than the refresh.  In that case, the server call to update
     * will happen before the refresh in all cases, so when the refresh runs, the local changes would've been posted already.
     *
     * @param message String message that can be tested by each command.  Similar to broadcast action.
     */
    public void onRuntimeMessage(Context context, String message)
    {
        onRuntimeMessage(context, message, null);
    }

    /**
     * Same as the other message method, but include some args.  Id, for example.
     * @param message
     * @param args
     */
    public void onRuntimeMessage(Context context, String message, Map args)
    {

    }

    public long getLastUpdate()
    {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public long getAdded()
    {
        return added;
    }

    public void setAdded(long added)
    {
        this.added = added;
    }

    public int getTransientExceptionCount()
    {
        return transientExceptionCount;
    }

    public void setTransientExceptionCount(int transientExceptionCount)
    {
        this.transientExceptionCount = transientExceptionCount;
    }

    public int compareTo(Command command)
    {
        int priorityCompare = command.getPriority() - priority;
        if (priorityCompare != 0)
        {
            return priorityCompare;
        }
        return (int)(added - command.getAdded());
    }

    private Long id;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }
}
