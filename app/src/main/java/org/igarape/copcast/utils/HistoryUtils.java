package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.exceptions.HistoryException;
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
            json.put("extras", extras);
        json.put("date", df.format(new Date()));
        return json;
    }

    public static void registerHistoryEvent(Context context, State currentState) throws HistoryException {
        registerHistoryEvent(context, currentState, (JSONObject) null);
    }

    public static void registerHistoryEvent(final Context context, State currentState, String extras) throws HistoryException {
        //events repeat current state
        registerHistory(context, currentState, currentState, extras);
    }

    public static void registerHistoryEvent(final Context context, State currentState, JSONObject extras) throws HistoryException {
        //events repeat current state
        registerHistory(context, currentState, currentState, extras);
    }

     public static void registerHistory(final Context context, State currentState, State newState) throws HistoryException {

        registerHistory(context, currentState, newState, (JSONObject) null);
    }

    public static void registerHistory(final Context context, State currentState, State newState, String extras) throws HistoryException {

        JSONObject obj = new JSONObject();

        try {
            obj = obj.put("extras", extras);
        } catch (JSONException e) {
            Log.e(TAG, "falhou", e);
        }

        registerHistory(context, currentState, newState, obj);
    }


    public static void registerHistory(final Context context, State currentState, State nextState, JSONObject extras) throws HistoryException {

        if (currentState == null)
            throw new HistoryException("Current state cannot be null");

        if (nextState == null)
            throw new HistoryException("Next state cannot be null");

        try {
            final JSONObject history = buildJson(currentState, nextState, extras);

            if (extras != null)
                extras.put("sessionId", Globals.getSessionID());

            final LoggedHTTPResponseCallback hlogger = new LoggedHTTPResponseCallback(context, JsonDataType.TYPE_HISTORY_DATA, history, TAG);

            NetworkUtils.post(context, JsonDataType.TYPE_HISTORY_DATA.getUrl(), history, hlogger);

        } catch (JSONException e) {
            Log.e(TAG, "error sending history", e);
        }
    }
}
