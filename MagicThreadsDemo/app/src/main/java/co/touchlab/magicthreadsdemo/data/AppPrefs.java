package co.touchlab.magicthreadsdemo.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by kgalligan on 9/13/14.
 */
public class AppPrefs
{
    public static final String CACHED_DATA = "CACHED_DATA";

    private static AppPrefs instance;

    private SharedPreferences prefs;

    public static synchronized AppPrefs getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new AppPrefs();
            instance.prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE);
        }

        return instance;
    }

    public String getCachedData()
    {
        return prefs.getString(CACHED_DATA, null);
    }

    public void setCachedData(String data)
    {
        prefs.edit().putString(CACHED_DATA, data).apply();
    }
}
