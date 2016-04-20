package org.igarape.copcast.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.exceptions.SqliteDbException;
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

        try {
            json.put("lat", Globals.getLastKnownLocation().getLatitude());
            json.put("lng", Globals.getLastKnownLocation().getLongitude());
        } catch (Exception ex) {
            Log.e(TAG, "Failed to fetch last known location. Sending empty incident.");
        }

        return json;
    }


    public static void registerIncident(final Context context) {

        // add delayed call to reset incident flag state;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable(){
            @Override
            public void run(){
                Log.d(TAG, "Resetting incident flag state to NOT_FLAGGED");
                Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);
            }
        }, 5000);

        saveIncident(context);
        try {
            sendIncident(context, buildJson());
        } catch (JSONException e) {
            Log.e(TAG, "Could not build incident json (date error?). Not sending any incident");
        }

    }

    private static void sendIncident(final Context context, final JSONObject incident) {

        failureLogged.set(false);

        final LoggedHTTPResponseCallback ilogger = new LoggedHTTPResponseCallback(context, JsonDataType.TYPE_INCIDENT_FLAG, incident, TAG);

        NetworkUtils.post(context, "/incidents", incident, ilogger);
    }

    public static void saveIncident(Context context) {

        try {
            JSONObject obj = new JSONObject();
            obj.put("date", TimeUtils.getTimestamp().toString());
            SqliteUtils.storeToDb(context, Globals.getUserLogin(context),
                    JsonDataType.TYPE_FLAGGED_VIDEO, obj.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error building incident JSON");
            Log.d(TAG, e.toString());
        } catch (SqliteDbException e) {
            Log.e(TAG, "error storing data into db", e);
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
