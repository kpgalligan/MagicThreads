package co.touchlab.android.threading.tasks.persisted.storage.sqlite;

import android.content.ContentValues;
import co.touchlab.android.threading.tasks.persisted.SuperbusProcessException;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/18/12
 * Time: 11:01 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SQLiteDatabaseIntf
{
    CursorIntf query(String tableName, String[] columnList);
    void execSQL(String sql)throws SuperbusProcessException;
    int delete(String tableName, String query, String[] params);
    long insertOrThrow(String tableName, String nullColHack, ContentValues values)throws SuperbusProcessException;
    int update(String tableName, ContentValues values, String whereClause, String[] whereArgs);
    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();
}
