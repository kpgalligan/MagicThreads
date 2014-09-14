package co.touchlab.magicthreadsdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import co.touchlab.magicthreadsdemo.data.DemoSimpleEvent;
import co.touchlab.magicthreadsdemo.loaders.EventLoader;


public class LoaderDemoActivity extends FragmentActivity
{
    public static void callMe(Activity a)
    {
        Intent i = new Intent(a, LoaderDemoActivity.class);
        a.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader_demo);

        getSupportLoaderManager().initLoader(0, null, new android.support.v4.app.LoaderManager.LoaderCallbacks<List<DemoSimpleEvent>>()
        {
            @Override
            public android.support.v4.content.Loader<List<DemoSimpleEvent>> onCreateLoader(int i, Bundle bundle)
            {
                return new EventLoader(LoaderDemoActivity.this);
            }

            @Override
            public void onLoadFinished(android.support.v4.content.Loader<List<DemoSimpleEvent>> listLoader, List<DemoSimpleEvent> demoSimpleEvents)
            {
                showResults(demoSimpleEvents);
            }

            @Override
            public void onLoaderReset(android.support.v4.content.Loader<List<DemoSimpleEvent>> listLoader)
            {

            }
        });
    }

    private void showResults(List<DemoSimpleEvent> demoSimpleEvents)
    {
        ListView eventList = (ListView) findViewById(R.id.eventList);
        eventList.setAdapter(new ArrayAdapter<DemoSimpleEvent>(this, android.R.layout.simple_list_item_1, demoSimpleEvents));
    }

}
