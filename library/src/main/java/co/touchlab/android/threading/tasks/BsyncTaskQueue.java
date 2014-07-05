package co.touchlab.android.threading.tasks;

import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by kgalligan on 7/5/14.
 */
public class BsyncTaskQueue
{
    private int taskIdCounter = 0;
    private Handler handler;
    private SparseArray<BsyncTaskSabot> taskMap = new SparseArray<BsyncTaskSabot>();
    private Map<Object, Set<Integer>> contextToTasks = new HashMap<Object, Set<Integer>>();

    public BsyncTaskQueue()
    {
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

    public synchronized void addTask(BsyncTask task)
    {
        task.setTaskId(taskIdCounter++);
        BsyncTaskSabot bsyncTaskSabot = new BsyncTaskSabot(task);
        taskMap.put(task.getTaskId(), bsyncTaskSabot);
        handler.post(bsyncTaskSabot);
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
            }
            catch (Exception e)
            {
                boolean handled = bsyncTask.handleError(e);
                if(!handled)
                {
                    throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
                }
            }
            finally
            {
                taskMap.delete(bsyncTask.getTaskId());
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
    }
}
