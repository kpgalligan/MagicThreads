package co.touchlab.android.threading.tasks;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by kgalligan on 7/5/14.
 */
public class TaskQueue
{
    private static Executor threadPool = Executors.newSingleThreadExecutor();

    public static void runProcess(Task runnable)
    {
        execute(runnable);
    }

    public interface Task
    {
        void run() throws Exception;

        /**
         * Handle Exception that occurred during processing.  Return true if handled, false if not.  If not handled, app will throw and probably crash.
         *
         * @param e
         * @return true if handled, false if not (which will throw it)
         */
        boolean handleError(Exception e);
    }

    private static void execute(final Task runnable)
    {
        threadPool.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    runnable.run();
                }
                catch (Exception e)
                {
                    boolean handled = runnable.handleError(e);
                    if (!handled)
                        throw new RuntimeException(e);
                }
            }
        });
    }
}
