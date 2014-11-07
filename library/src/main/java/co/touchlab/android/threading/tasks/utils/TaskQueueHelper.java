package co.touchlab.android.threading.tasks.utils;

import co.touchlab.android.threading.tasks.Task;
import co.touchlab.android.threading.tasks.TaskQueue;

/**
 * Created by kgalligan on 10/10/14.
 */
public class TaskQueueHelper
{
    public static boolean hasTasksOfType(TaskQueue taskQueueActual, Class... classes)
    {
        ClassesQuery queueQuery = new ClassesQuery(classes);
        taskQueueActual.query(queueQuery);
        return queueQuery.found;
    }

    static class ClassesQuery implements TaskQueue.QueueQuery
    {
        boolean found = false;
        Class[] classes;

        ClassesQuery(Class[] classes)
        {
            this.classes = classes;
        }

        @Override
        public void query(Task task)
        {
            for (Class aClass : classes)
            {
                if(task.getClass().equals(aClass))
                {
                    found = true;
                    break;
                }
            }
        }
    }
}
