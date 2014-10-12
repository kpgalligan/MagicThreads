package co.touchlab.android.threading.tasks;

/**
 * Created by kgalligan on 10/10/14.
 */
public class TaskQueueHelper
{
    public static boolean hasTasksOfType(TaskQueueActual taskQueueActual, Class... classes)
    {
        ClassesQuery queueQuery = new ClassesQuery(classes);
        taskQueueActual.query(queueQuery);
        return queueQuery.found;
    }

    static class ClassesQuery implements TaskQueueActual.QueueQuery
    {
        boolean found = false;
        Class[] classes;

        ClassesQuery(Class[] classes)
        {
            this.classes = classes;
        }

        @Override
        public void query(TaskQueue.Task task)
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
