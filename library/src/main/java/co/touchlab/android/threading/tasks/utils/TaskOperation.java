package co.touchlab.android.threading.tasks.utils;

import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.touchlab.android.threading.tasks.TaskQueue;

/**
 * Created by kgalligan on 10/11/14.
 */
public class TaskOperation
{
    List<Class>         matchClasses   = new ArrayList<Class>(2);
    List<ViewOperation> viewOperations = new ArrayList<ViewOperation>(10);

    public void addClasses(Class... classes)
    {
        matchClasses.addAll(Arrays.asList(classes));
    }

    public void addOperation(ViewOperation operation)
    {
        viewOperations.add(operation);
    }

    public void enabled(View... views)
    {
        for(View view : views)
        {
            addOperation(new ViewEnabled(view));
        }
    }

    public void visible(boolean normallyVisible, View... views)
    {
        for(View view : views)
        {
            addOperation(new ViewVisible(normallyVisible, view));
        }
    }

    public void visible(boolean normallyVisible, boolean hideGone, View... views)
    {
        for(View view : views)
        {
            addOperation(new ViewVisible(normallyVisible, hideGone, view));
        }
    }

    public void perform(TaskQueue queueActual)
    {
        boolean found = TaskQueueHelper
                .hasTasksOfType(queueActual, matchClasses.toArray(new Class[matchClasses.size()]));
        for(ViewOperation viewOperation : viewOperations)
        {
            viewOperation.perform(found);
        }
    }

    public abstract class ViewOperation
    {
        View view;

        protected ViewOperation(View view)
        {
            this.view = view;
        }

        abstract void perform(boolean found);
    }

    class ViewEnabled extends ViewOperation
    {
        protected ViewEnabled(View view)
        {
            super(view);
        }

        @Override
        void perform(boolean found)
        {
            view.setEnabled(! found);
        }
    }

    class ViewVisible extends ViewOperation
    {
        boolean normallyShown;
        boolean hideGone;

        ViewVisible(boolean normallyShown, View view)
        {
            this(normallyShown, true, view);
        }

        ViewVisible(boolean normallyShown, boolean hideGone, View view)
        {
            super(view);
            this.normallyShown = normallyShown;
            this.hideGone = hideGone;
        }

        @Override
        void perform(boolean found)
        {
            int hideState = hideGone
                    ? View.GONE
                    : View.INVISIBLE;
            int vis;
            if(normallyShown)
            {
                vis = found
                        ? hideState
                        : View.VISIBLE;
            }
            else
            {
                vis = found
                        ? View.VISIBLE
                        : hideState;
            }

            view.setVisibility(vis);
        }
    }
}