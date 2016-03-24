package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.state.State;
import org.igarape.copcast.state.State;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by brunosiqueira on 23/09/2014.
 */
public class HistoryUtils {

    private static final String TAG = HistoryUtils.class.getName();

    public static JSONObject buildJson(State currentState, State nextState, JSONObject extras) throws JSONException {
        JSONObject json = new JSONObject();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
        json.put("previousState", currentState.toString());
        json.put("nextState", nextState.toString());
        if (extras != null)
            json.put("extras", extras.toString());
        json.put("date", df.format(new Date()));
        return json;
    }

    public static void registerHistoryEvent(Context context, State currentState){
        registerHistoryEvent(context, currentState, (JSONObject) null);
    }

    public static void registerHistoryEvent(final Context context, State currentState, String extras) {
        registerHistory(context, currentState, currentState, extras);
    }

    public static void registerHistoryEvent(final Context context, State currentState, JSONObject extras) {
        registerHistory(context, currentState, currentState, extras);
    }

     public static void registerHistory(final Context context, State currentState, State newState) {

        registerHistory(context, currentState, newState, (JSONObject) null);
    }

    public static void registerHistory(final Context context, State currentState, State newState, String extras) {

        JSONObject obj = null;

        try {
            obj = new JSONObject(extras);
        } catch (JSONException e) {
            try {
                obj = new JSONObject('"'+extras+'"');
            } catch (JSONException e2) {
                Log.d(TAG, extras);
                Log.e(TAG, "Error converting String to JsonObject", e2);
            }
        }

        registerHistory(context, currentState, newState, obj);
    }


    public static void registerHistory(final Context context, State currentState, State nextState, JSONObject extras) {

        try {
            final JSONObject history = buildJson(currentState, nextState, extras);

            if (extras != null && extras.get("sessionId") == null)
                extras.put("sessionId", Globals.getSessionID());

            final LoggedHTTPResponseCallback hlogger = new LoggedHTTPResponseCallback(context, JsonDataType.TYPE_HISTORY_DATA, history, TAG);

            NetworkUtils.post(context, JsonDataType.TYPE_HISTORY_DATA.getUrl(), history, hlogger);

        } catch (JSONException e) {
            Log.e(TAG, "error sending history", e);
        }
    }
}
