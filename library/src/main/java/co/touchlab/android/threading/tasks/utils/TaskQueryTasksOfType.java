package co.touchlab.android.threading.tasks.utils;

import co.touchlab.android.threading.tasks.BaseTaskQueue;
import co.touchlab.android.threading.tasks.Task;
import co.touchlab.android.threading.tasks.TaskQueue;

/**
 * Created by kgalligan on 9/13/14.
 */
public class TaskQueryTasksOfType implements TaskQueue.QueueQuery
{
    public  boolean found;
    private Class   cls;

    public TaskQueryTasksOfType(Class cls)
    {
        this.cls = cls;
    }

    @Override
    public void query(BaseTaskQueue queue, Task task)
    {
        if(task.getClass().equals(cls))
        {
            found = true;
        }
    }
}
