package org.igarape.copcast.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.igarape.copcast.db.JsonDataContract.JsonDataEntry;
import org.igarape.copcast.db.JsonDataDbHelper;
import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.exceptions.SqliteDbException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SqliteUtils {
    private static final String TAG = SqliteUtils.class.getName();

    public static SQLiteDatabase getWriteDb(Context context) {
        JsonDataDbHelper dbHelper = new JsonDataDbHelper(context);
        return dbHelper.getWritableDatabase();
    }

    public static SQLiteDatabase getReadDb(Context context) {
        JsonDataDbHelper dbHelper = new JsonDataDbHelper(context);
        return dbHelper.getWritableDatabase();
    }

    public static long storeToDb(Context context, String user, JsonDataType type, String value) throws SqliteDbException {

        if (user == null)
            throw new SqliteDbException(TAG, "Invalid user: null");

        if (type == null)
            throw new SqliteDbException(TAG, "Invalid type: null");

        if (value == null)
            throw new SqliteDbException(TAG, "Invalid value: null");

        long newRowId;

        SQLiteDatabase db = SqliteUtils.getWriteDb(context);

        ContentValues values = new ContentValues();
        values.put(JsonDataEntry.COLUMN_USER, user);
        values.put(JsonDataEntry.COLUMN_TYPE, type.getType());
        values.put(JsonDataEntry.COLUMN_DATA, value);

        newRowId = db.insert(JsonDataEntry.TABLE_NAME, null, values);

        Log.d(TAG, "INSERTED "+value+" OF TYPE "+type.getType()+" FOR USER "+user+" WITH ID "+newRowId);
        db.close();
        return newRowId;
    }

    public static long storeToDb(Context context, String user, JsonDataType type, JSONObject obj) throws SqliteDbException {
        return storeToDb(context, user, type, obj.toString());
    }

    public static void clearByType(Context context, String user, JsonDataType jsonDataType) {
        SQLiteDatabase db = SqliteUtils.getWriteDb(context);

        int r = db.delete(JsonDataEntry.TABLE_NAME, JsonDataEntry.COLUMN_TYPE+"=? AND "+JsonDataEntry.COLUMN_USER+"=?", new String[] {jsonDataType.getType(), user});

        Log.d(TAG, "Number of " + jsonDataType + " entries removed: " + r);
        db.close();
    }

    public static void clearById(Context context, long id) {
        SQLiteDatabase db = SqliteUtils.getWriteDb(context);

        try {
            int r = db.delete(JsonDataEntry.TABLE_NAME, JsonDataEntry._ID + "=?", new String[]{String.valueOf(id)});
            Log.v(TAG, "Entry ID [" + id + "] removed: " + r);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting entry with id: " + id, e);
        }
        db.close();
    }

    public static JSONArray getFromDb(Context context, String user, JsonDataType type) throws JSONException {
        SQLiteDatabase db = SqliteUtils.getReadDb(context);

        String[] projection = {
                JsonDataEntry.COLUMN_DATA
        };

        Cursor c = db.query(
                JsonDataEntry.TABLE_NAME,
                projection,
                JsonDataEntry.COLUMN_USER+"=? AND " +
                        JsonDataEntry.COLUMN_TYPE+"=?",
                new String[] {user, type.getType()},
                null,
                null,
                JsonDataEntry.COLUMN_TIMESTAMP+" DESC"
        );

        JSONArray ret = new JSONArray();

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String tmp = c.getString(c.getColumnIndexOrThrow(JsonDataEntry.COLUMN_DATA));
            Log.d(TAG, "Got: "+tmp);
            JSONObject obj = new JSONObject(tmp);
            ret.put(obj);
        }

        c.close();
        db.close();

        return ret;
    }

    public static ArrayList<String> getUsersInDb(Context context) {
        SQLiteDatabase db = SqliteUtils.getReadDb(context);

        String[] projection = {
                JsonDataEntry.COLUMN_USER
        };

        Cursor c = db.query(true,
                JsonDataEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ArrayList<String> ret = new ArrayList<>();

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String tmp = c.getString(c.getColumnIndexOrThrow(JsonDataEntry.COLUMN_USER));
            Log.d(TAG, "Got: "+tmp);
            ret.add(tmp);
        }

        db.close();

        return ret;
    }

    public static void dumpTypesFromUser(Context context, String user) {
        SQLiteDatabase db = SqliteUtils.getReadDb(context);

        String username = user != null ? user : "null";

        Cursor c = db.rawQuery(
                "SELECT distinct(" + JsonDataEntry.COLUMN_TYPE + ") FROM " + JsonDataEntry.TABLE_NAME + " WHERE " +
                        JsonDataEntry.COLUMN_USER + "= ?",
                new String[]{username}
        );

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String tmp = c.getString(0);
            Log.d(TAG, "Type for user("+user+"): "+tmp);
        }

        db.close();
    }

    public static int countEntries(Context context) {
        SQLiteDatabase db = SqliteUtils.getReadDb(context);

        Cursor c = db.rawQuery(
                "SELECT count(*) FROM " + JsonDataEntry.TABLE_NAME,
                null
        );

        c.moveToFirst();

        int res = c.getInt(0);

        db.close();

        Log.d(TAG, "Entries in sqlite: "+res);
        return res;
    }
}
