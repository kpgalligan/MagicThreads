package co.touchlab.android.threading.tasks.persisted;


import co.touchlab.android.threading.errorcontrol.SoftException;

/**
 * Command processing errors will eventually need to purge commands and signal processing errors. Policy
 * defines how this purging is handled.
 *
 * User: kgalligan
 * Date: 10/13/12
 * Time: 3:23 AM
 */
public interface CommandPurgePolicy
{
    boolean purgeCommandOnTransientException(Command command, SoftException exception);
}
