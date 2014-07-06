package co.touchlab.android.threading.tasks;

import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import de.greenrobot.event.EventBus;

import java.util.*;

/**
 * Created by kgalligan on 7/5/14.
 */
public class BsyncTaskQueue
{
    /*private final Handler mainHandler;
    private Handler handler;
    private Map<Object, Collection<BsyncTaskSabot>> contextToTasks = new HashMap<Object, Collection<BsyncTaskSabot>>();

    public BsyncTaskQueue()
    {
        mainHandler = new Handler();
        LooperThread looperThread = new LooperThread();
        looperThread.start();
        try
        {
            looperThread.join();
        }
        catch (InterruptedException e)
        {
        }
    }

    public synchronized void addTask(Object host, BsyncTask task)
    {
        Collection<BsyncTaskSabot> bsyncTaskSabots = findAddSabotCollection(host);

        BsyncTaskSabot bsyncTaskSabot = new BsyncTaskSabot(task);
        bsyncTaskSabots.add(bsyncTaskSabot);

        handler.post(bsyncTaskSabot);
    }

    public synchronized void removeTasks(Object host)
    {
        Collection<BsyncTaskSabot> bsyncTaskSabots = contextToTasks.get(host);
        if(bsyncTaskSabots != null)
        {
            contextToTasks.remove(host);
            mainHandler.postAtFrontOfQueue(new Runnable()
            {
                @Override
                public void run()
                {
                    finish this.  A mess
                }
            });
            for (BsyncTaskSabot bsyncTaskSabot : bsyncTaskSabots)
            {
                handler.removeCallbacks(bsyncTaskSabot);
            }
        }
    }

    private synchronized void removeTask(BsyncTaskSabot bsyncTaskSabot)
    {
        boolean found = false;

        for (Object host : contextToTasks.keySet())
        {
            Collection<BsyncTaskSabot> bsyncTaskSabots = contextToTasks.get(host);
            for (BsyncTaskSabot taskSabot : bsyncTaskSabots)
            {
                if (taskSabot == bsyncTaskSabot)
                {
                    found = true;
                    handler.removeCallbacks(taskSabot);
                    bsyncTaskSabots.remove(bsyncTaskSabot);
                    if (bsyncTaskSabots.size() == 0)
                    {
                        contextToTasks.remove(host);
                    }
                }
                if (found)
                    break;
            }
            if (found)
                break;
        }
    }

    private Collection<BsyncTaskSabot> findAddSabotCollection(Object host)
    {
        Collection<BsyncTaskSabot> bsyncTaskSabots = contextToTasks.get(host);
        if (bsyncTaskSabots == null)
        {
            bsyncTaskSabots = new HashSet<BsyncTaskSabot>();
            contextToTasks.put(host, bsyncTaskSabots);
        }
        return bsyncTaskSabots;
    }

    class BsyncTaskSabot implements Runnable
    {
        final BsyncTask bsyncTask;

        BsyncTaskSabot(BsyncTask bsyncTask)
        {
            this.bsyncTask = bsyncTask;
        }

        @Override
        public void run()
        {
            try
            {
                bsyncTask.doInBackground();
                mainHandler.post(new )
            }
            catch (Exception e)
            {
                boolean handled = bsyncTask.handleError(e);
                if (!handled)
                {
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
            }
            finally
            {
                removeTask(this);
            }
        }
    }

    class LooperThread extends Thread
    {
        public void run()
        {
            Looper.prepare();

            handler = new Handler();

            Looper.loop();
        }
    }*/
}
