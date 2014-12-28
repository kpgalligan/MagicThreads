package co.touchlab.android.threading.tasks.utils;

import co.touchlab.android.threading.tasks.Task;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.android.threading.tasks.sticky.StickyTask;
import co.touchlab.android.threading.tasks.sticky.StickyTaskManager;

/**
 * Created by kgalligan on 10/10/14.
 */
public class TaskQueueHelper
{
    public static boolean hasTasksOfType(TaskQueue taskQueueActual, Class... classes)
    {
        return hasTasksOfType(null, taskQueueActual, classes);
    }

    public static boolean hasTasksOfType(StickyTaskManager stickyTaskManager, TaskQueue taskQueueActual, Class... classes)
    {
        ClassesQuery queueQuery = new ClassesQuery(stickyTaskManager, classes);
        taskQueueActual.query(queueQuery);
        return queueQuery.found;
    }

    static class ClassesQuery implements TaskQueue.QueueQuery
    {
        boolean found = false;
        Class[] classes;
        StickyTaskManager stickyTaskManager;

        ClassesQuery(StickyTaskManager stickyTaskManager, Class[] classes)
        {
            this.stickyTaskManager = stickyTaskManager;
            this.classes = classes;
        }

        @Override
        public void query(Task task)
        {
            for (Class aClass : classes)
            {
                if (task.getClass().equals(aClass))
                {
                    if(stickyTaskManager != null && task instanceof StickyTask)
                    {
                        StickyTask stickyTask = (StickyTask) task;
                        if(stickyTaskManager.isTaskForMe(stickyTask))
                        {
                            found = true;
                            break;
                        }
                    }
                    else
                    {
                        found = true;
                        break;
                    }
                }
            }
        }
    }
}
