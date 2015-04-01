package co.touchlab.android.threading.tasks.persisted.storage.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import co.touchlab.android.threading.tasks.persisted.CommandPersistenceProvider;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/22/13
 * Time: 5:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleDatabaseHelper extends SQLiteOpenHelper
{
    private final static int VERSION = 1;

    private static SimpleDatabaseHelper INSTANCE;

    public static synchronized SimpleDatabaseHelper getInstance(Context context, String fileName)
    {
        if(INSTANCE == null)
        {
            INSTANCE = new SimpleDatabaseHelper(context.getApplicationContext(), fileName);
        }

        return INSTANCE;
    }

    private SimpleDatabaseHelper(Context context, String fileName)
    {
        super(context, fileName, null, VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db)
    {
        super.onOpen(db);
        // @reminder Must enable foreign keys as it's off by default
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        CommandPersistenceProvider.createTables(new ClearSQLiteDatabase(db));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }
}
