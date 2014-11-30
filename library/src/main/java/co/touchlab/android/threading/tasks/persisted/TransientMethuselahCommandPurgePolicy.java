package co.touchlab.android.threading.tasks.persisted;

import co.touchlab.android.threading.errorcontrol.SoftException;

/**
 * Commands that trigger TransientException will live forever.
 * <p/>
 * This is the default, and in general models desired
 * functionality.  However, this is dangerous.  If something triggers TransientException, but never resolves itself,
 * the queue will be forever blocked.
 * <p/>
 * However, in real world scenarios, a device may have a spotty connection, or be offline for several days, or both.
 * Killing commands based on network issues alone could cause serious issues with app functionality.
 * <p/>
 * User: kgalligan
 * Date: 10/13/12
 * Time: 3:32 AM
 */
public class TransientMethuselahCommandPurgePolicy implements CommandPurgePolicy
{
    @Override
    public boolean purgeCommandOnTransientException(Command command, SoftException exception)
    {
        return false;
    }
}
