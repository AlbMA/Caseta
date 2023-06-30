package com.example.alberto.caseta;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBManager {

    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(String name, String filename, String image, String desc) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.NAME, name);
        contentValue.put(DatabaseHelper.FILENAME, filename);
        contentValue.put(DatabaseHelper.IMAGE, image);
        contentValue.put(DatabaseHelper.DESC, desc);
        database.insert(DatabaseHelper.TABLE_NAME, null, contentValue);
    }

    public String check(String filename) {
        String[] columns = new String[] { DatabaseHelper.NAME};
        String whereClause = DatabaseHelper.FILENAME + " = ?";
        String[] whereArgs = new String[] { filename };

        Cursor c = database.query(DatabaseHelper.TABLE_NAME, columns, whereClause, whereArgs,
                null, null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            return c.getString(0);
        }
        return null;
    }

    public String[] getNameDescbyFilename(String filename) {
        String[] columns = new String[] {DatabaseHelper.NAME, DatabaseHelper.DESC};
        String whereClause = DatabaseHelper.FILENAME + " = ?";
        String[] whereArgs = new String[] { filename };
        String[] results = new String[] {"No name", "No description"};

        Cursor c = database.query(DatabaseHelper.TABLE_NAME, columns, whereClause, whereArgs,
                null, null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            results[0] = c.getString(0);
            results[1] = c.getString(1);
            return results;
        }
        return null;
    }

    public Cursor fetch() {
        String[] columns = new String[] { DatabaseHelper._ID, DatabaseHelper.FILENAME,  DatabaseHelper.IMAGE, DatabaseHelper.DESC };
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, columns, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int update(long _id, String filename, String name, String image, String desc) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.NAME, name);
        contentValues.put(DatabaseHelper.FILENAME, filename);
        contentValues.put(DatabaseHelper.IMAGE, image);
        contentValues.put(DatabaseHelper.DESC, desc);
        int i = database.update(DatabaseHelper.TABLE_NAME, contentValues, DatabaseHelper._ID + " = " + _id, null);
        return i;
    }

    public int update(String filename, String name, String desc) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.NAME, name);
        contentValues.put(DatabaseHelper.DESC, desc);
        int i = database.update(DatabaseHelper.TABLE_NAME,
                contentValues,
                DatabaseHelper.FILENAME + " = ?",
                new String[]{filename});
        return i;
    }

    public void delete(long _id) {
        database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper._ID + "=" + _id, null);
    }

}
