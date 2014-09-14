package co.touchlab.magicthreadsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;


public class OptionsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
    }

    public void loaderExample(View v)
    {
        LoaderDemoActivity.callMe(this);
    }

    public void taskExample(View v)
    {
        TaskDemoActivity.callMe(this);
    }

    public void doubleTapLoaderExample(View v)
    {
        DoubleTapLoaderDemoActivity.callMe(this);
    }
}
