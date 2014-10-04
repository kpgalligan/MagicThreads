package co.touchlab.android.threading.tasks.persisted.storage.sqlite;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/22/13
 * Time: 5:33 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CursorIntf
{
    boolean moveToNext();

    void close();

    long getLong(int i);

    String getString(int i);
}
