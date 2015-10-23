package org.igarape.copcast.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.igarape.copcast.db.JsonDataContract.JsonDataEntry;
import org.igarape.copcast.db.JsonDataDbHelper;
import org.igarape.copcast.db.JsonDataType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    public static void storeToDb(Context context, String user, String type, String value) {
        long newRowId;

        SQLiteDatabase db = SqliteUtils.getWriteDb(context);

        ContentValues values = new ContentValues();
        values.put(JsonDataEntry.COLUMN_USER, user);
        values.put(JsonDataEntry.COLUMN_TYPE, type);
        values.put(JsonDataEntry.COLUMN_DATA, value);

        newRowId = db.insert(JsonDataEntry.TABLE_NAME, null, values);

        Log.d(TAG, "INSERTED "+value+" OF TYPE "+type+" FOR USER "+user+" WITH ID "+newRowId);
        db.close();
    }

    public static void storeToDb(Context context, String user, String type, JSONObject obj) {
        storeToDb(context, user, type, obj.toString());
    }

    public static void clearByType(Context context, String user, String jsonDataType) {
        SQLiteDatabase db = SqliteUtils.getWriteDb(context);

        int r = db.delete(JsonDataEntry.TABLE_NAME, JsonDataEntry.COLUMN_TYPE+"=? AND "+JsonDataEntry.COLUMN_USER+"=?", new String[] {jsonDataType, user});

        Log.d(TAG, "Number of " + jsonDataType + " entries removed: " + r);
        db.close();
    }

    public static JSONArray getFromDb(Context context, String user, String type) throws JSONException {
        SQLiteDatabase db = SqliteUtils.getReadDb(context);

        String[] projection = {
                JsonDataEntry.COLUMN_DATA
        };

        Cursor c = db.query(
                JsonDataEntry.TABLE_NAME,
                projection,
                JsonDataEntry.COLUMN_USER+"=? AND " +
                        JsonDataEntry.COLUMN_TYPE+"=?",
                new String[] {user, type},
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

        db.close();

        return ret;
    }
}
