package co.touchlab.android.threading.tasks.persisted.storage.sqlite;


import android.content.ContentValues;
import co.touchlab.android.superbus.errorcontrol.StorageException;
import net.sqlcipher.Cursor;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/18/12
 * Time: 11:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class EncryptedSQLiteDatabase implements SQLiteDatabaseIntf
{
    private SQLiteDatabase db;

    public static class EncryptedCursor implements CursorIntf
    {
        private Cursor cursor;

        public EncryptedCursor(Cursor cursor)
        {
            this.cursor = cursor;
        }

        @Override
        public boolean moveToNext()
        {
            return cursor.moveToNext();
        }

        @Override
        public void close()
        {
            cursor.close();
        }

        @Override
        public long getLong(int i)
        {
            return cursor.getLong(i);
        }

        @Override
        public String getString(int i)
        {
            return cursor.getString(i);
        }
    }

    public EncryptedSQLiteDatabase(SQLiteDatabase db)
    {
        this.db = db;
    }

    @Override
    public CursorIntf query(String tableName, String[] columnList)
    {
        return new EncryptedCursor(db.query(tableName, columnList, null, null, null, null, null));
    }

    @Override
    public void execSQL(String sql) throws StorageException
    {
        try
        {
            db.execSQL(sql);
        }
        catch (SQLException e)
        {
            throw new StorageException(e);
        }
    }

    @Override
    public int delete(String tableName, String query, String[] params)
    {
        return db.delete(tableName, query, params);
    }

    @Override
    public long insertOrThrow(String tableName, String nullColHack, ContentValues values) throws StorageException
    {
        try
        {
            return db.insertOrThrow(tableName, nullColHack, values);
        }
        catch (SQLException e)
        {
            throw new StorageException(e);
        }
    }

    @Override
    public int update(String tableName, ContentValues values, String whereClause, String[] whereArgs)
    {
        return db.update(tableName, values, whereClause, whereArgs);
    }

    @Override
    public void beginTransaction()
    {
        db.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful()
    {
        db.setTransactionSuccessful();
    }

    @Override
    public void endTransaction()
    {
        db.endTransaction();
    }
}
