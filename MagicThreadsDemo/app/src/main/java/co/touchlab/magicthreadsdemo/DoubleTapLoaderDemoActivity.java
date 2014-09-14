package co.touchlab.magicthreadsdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import co.touchlab.android.threading.loaders.networked.DoubleTapResult;
import co.touchlab.magicthreadsdemo.data.DemoSimpleEvent;
import co.touchlab.magicthreadsdemo.loaders.EventDoubleTapLoader;


public class DoubleTapLoaderDemoActivity extends FragmentActivity
{

    private static ListView eventList;
    private View spinner;

    public static void callMe(Activity a)
    {
        Intent i = new Intent(a, DoubleTapLoaderDemoActivity.class);
        a.startActivity(i);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_double_tap_loader_demo);

        eventList = (ListView) findViewById(R.id.eventList);
        spinner = findViewById(R.id.spinner);

        getSupportLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<DoubleTapResult<List<DemoSimpleEvent>, Integer>>()
        {

            @Override
            public Loader<DoubleTapResult<List<DemoSimpleEvent>, Integer>> onCreateLoader(int i, Bundle bundle)
            {
                return new EventDoubleTapLoader(DoubleTapLoaderDemoActivity.this);
            }

            @Override
            public void onLoadFinished(Loader<DoubleTapResult<List<DemoSimpleEvent>, Integer>> doubleTapResultLoader, DoubleTapResult<List<DemoSimpleEvent>, Integer> result)
            {
                switch (result.getStatus())
                {
                    case Waiting:
                        showWaiting();
                        break;
                    case Data:
                        showResults(result.getResult());
                        break;
                    case NoData:
                        //Show no data
                        break;
                    case Error:
                        //Show error
                        break;
                    default:
                        throw new UnsupportedOperationException("Weird");
                }

            }

            @Override
            public void onLoaderReset(Loader<DoubleTapResult<List<DemoSimpleEvent>, Integer>> doubleTapResultLoader)
            {

            }
        });
    }

    private void showWaiting()
    {
        spinner.setVisibility(View.VISIBLE);
        eventList.setVisibility(View.GONE);
    }


    private void showResults(List<DemoSimpleEvent> demoSimpleEvents)
    {
        spinner.setVisibility(View.GONE);
        eventList.setVisibility(View.VISIBLE);

        ListView eventList = (ListView) findViewById(R.id.eventList);
        eventList.setAdapter(new ArrayAdapter<DemoSimpleEvent>(this, android.R.layout.simple_list_item_1, demoSimpleEvents));
    }

}
