package co.touchlab.android.threading.tasks;

/**
 * Created by kgalligan on 9/13/14.
 */
public class TaskQueryTasksOfType implements TaskQueueActual.QueueQuery
{
    public boolean found;
    private Class cls;

    public TaskQueryTasksOfType(Class cls) {
        this.cls = cls;
    }

    @Override
    public void query(TaskQueue.Task task)
    {
        if(task.getClass().equals(cls))
        {
            found = true;
        }
    }
}
