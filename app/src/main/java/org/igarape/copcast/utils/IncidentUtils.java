package org.igarape.copcast.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.state.IncidentFlagState;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

public class IncidentUtils {

    private static AtomicBoolean failureLogged = new AtomicBoolean(false);
    private static final String TAG = IncidentUtils.class.getName();

    public static JSONObject buildJson() throws JSONException {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);

        JSONObject json = new JSONObject();
        json.put("date", df.format(new Date()));
        json.put("lat", Globals.getLastKnownLocation().getLatitude());
        json.put("lng", Globals.getLastKnownLocation().getLongitude());
        return json;
    }

    public static void registerIncident(final Context context, final String videoPath) {

        final JSONObject[] incident = new JSONObject[1];

        saveVideoPath(context, videoPath);
        try {
            incident[0] = buildJson();
            sendIncident(context, incident[0]);
        } catch (Exception e) {
            Log.e(TAG, "error building incident JSON. Replay in 2s.");
            Log.d(TAG, e.toString());
            //Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED); // this will reset the flag state and allow for a longer volume press to take place;

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try{
                        Thread.sleep(2000);
                        Log.d(TAG, "TRYING AGAIN");
                        incident[0] = buildJson();
                        sendIncident(context, incident[0]);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed second try");
                        Log.d(TAG, e.toString());
                    }
                    return null;
                }

            }.execute();

        }

    }

    private static void sendIncident(final Context context, final JSONObject incident) {

        failureLogged.set(false);

        final GenericSqliteLogger ilogger = new GenericSqliteLogger(context, JsonDataType.TYPE_INCIDENT_FLAG, incident, TAG);

        NetworkUtils.post(context, "/incidents", incident, new HttpResponseCallback() {

            @Override
            public void unauthorized() {
                ilogger.logFailedData("unauthorized");
            }

            @Override
            public void failure(int statusCode) {
                ilogger.logFailedData("failure");
            }

            @Override
            public void noConnection() {
                ilogger.logFailedData("noConnection");
            }

            @Override
            public void badConnection() {
                ilogger.logFailedData("badConnection");
            }

            @Override
            public void badRequest() {
                ilogger.logFailedData("badRequest");
            }

            @Override
            public void badResponse() {
                ilogger.logFailedData("badResponse");
            }

            @Override
            public void success(byte[] output) {
                Log.d(TAG, "enviado!!!");
            }
        });
    }

    public static void saveVideoPath(Context context, String videoPath) {

        if (videoPath == null || videoPath.length() == 0) {
            Log.w(TAG, "saveVideoPath called without video argument");
            return;
        }

        try {
            JSONObject obj = new JSONObject();
            obj.put("date", TimeUtils.getTimestamp().toString());
            obj.put("videoPath", videoPath);
            SqliteUtils.storeToDb(context, Globals.getUserLogin(context),
                    JsonDataType.TYPE_FLAGGED_VIDEO, obj.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error building incident JSON. Video name not stored.");
            Log.d(TAG, e.toString());
        }
    }

    public static List<String> getFlaggedVideosList(Context context) throws JSONException {

        List<String> ret = new ArrayList<>();

        JSONArray jsonArray = SqliteUtils.getFromDb(context, Globals.getUserLogin(context), JsonDataType.TYPE_FLAGGED_VIDEO);

        for(int i=0; i<jsonArray.length(); i++)
            ret.add(jsonArray.getJSONObject(i).getString("videoPath"));

        return ret;
    }
}
