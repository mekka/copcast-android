package org.igarape.copcast.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.igarape.copcast.db.IncidentVideoDbHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.igarape.copcast.db.IncidentVideoContract.IncidentVideoEntry;

public class IncidentUtils {

    private static AtomicBoolean failureLogged = new AtomicBoolean(false);
    private static final String TAG = IncidentUtils.class.getName();

    public static JSONObject buildJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("timestamp", TimeUtils.getTimestamp().toString());
        return json;
    }


    public static ArrayList<String> getIncidentVideosList(String userLogin) {
        ArrayList<String> ret = new ArrayList<>();
        JSONArray array = FileUtils.getFileContentAsJsonArray(userLogin, FileUtils.INCIDENTS_TXT);

        try {
            for(int i=0; i<array.length(); i++) {
                ret.add(array.getJSONObject(i).getString("videoPath"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: "+e);
        }

        return ret;
    }

    private static void failedRegisterIncident(Context context, JSONObject incident, String tag) {
        Log.e(TAG, "incident not sent successfully: " + tag);
        if (!failureLogged.getAndSet(true)) {
            String userLogin = Globals.getUserLogin(context);
            FileUtils.LogIncident(userLogin, incident);
        }
    }

    public static void registerIncident(final Context context, final String videoPath) {

        final JSONObject incident;

        saveVideoPath(context, videoPath);

        try {
            incident = buildJson();
        } catch (JSONException e) {
            Log.e(TAG, "error building incident JSON");
            return;
        }

        failureLogged.set(false);

        NetworkUtils.post(context, "/incidents", incident, new HttpResponseCallback() {

            @Override
            public void unauthorized() {
                failedRegisterIncident(context, incident, "unauthorized");
            }

            @Override
            public void failure(int statusCode) {
                failedRegisterIncident(context, incident, "failure");
            }

            @Override
            public void noConnection() {
                failedRegisterIncident(context, incident, "noConnection");
            }

            @Override
            public void badConnection() {
                failedRegisterIncident(context, incident, "badConnection");
            }

            @Override
            public void badRequest() {
                failedRegisterIncident(context, incident, "badRequest");
            }

            @Override
            public void badResponse() {
                failedRegisterIncident(context, incident, "badResponse");
            }
        });
    }

    private static SQLiteDatabase getWriteDb(Context context) {
        IncidentVideoDbHelper dbHelper = new IncidentVideoDbHelper(context);
        return dbHelper.getWritableDatabase();
    }

    private static SQLiteDatabase getReadDb(Context context) {
        IncidentVideoDbHelper dbHelper = new IncidentVideoDbHelper(context);
        return dbHelper.getWritableDatabase();
    }

    private static void saveVideoPath(Context context, String videoPath) {

        SQLiteDatabase db = getWriteDb(context);

        ContentValues values = new ContentValues();
        values.put(IncidentVideoEntry.COLUMN_VIDEO_NAME, videoPath);

        long newRowId;
        newRowId = db.insert(IncidentVideoEntry.TABLE_NAME, null, values);

    }

    public static List<String> getEntries(Context context) {
        SQLiteDatabase db = getReadDb(context);

// Define a projection that specifies which columns from the database
// you will actually use after this query.
        String[] projection = {
                IncidentVideoEntry.COLUMN_VIDEO_NAME
        };

// How you want the results sorted in the resulting Cursor
        Cursor c = db.query(
                IncidentVideoEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        List<String> ret = new ArrayList<>();

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            ret.add(c.getString(c.getColumnIndexOrThrow(IncidentVideoEntry.COLUMN_VIDEO_NAME)));
        }

        return ret;
    }
}
