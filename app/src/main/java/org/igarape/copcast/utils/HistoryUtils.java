package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

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

    interface HLogger {
        public void log(String msg);
    }

    private static final String TAG = HistoryUtils.class.getName();

    public static JSONObject buildJson(State currentState, State nextState, String extras) throws JSONException {
        JSONObject json = new JSONObject();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
        json.put("previousState", currentState.toString());
        json.put("nextState", nextState.toString());
        json.put("extras", extras);
        json.put("date", df.format(new Date()));
        return json;
    }
    public static void registerHistory(Context context, State currentState, State nextState, final String userLogin){
        registerHistory(context, currentState, nextState, userLogin, null);
    }

    public static void registerHistory(Context context, State currentState, State nextState, final String userLogin, String extras) {

        try {
            final JSONObject history = buildJson(currentState, nextState, extras);

            final HLogger hlogger = new HLogger() {
                public void log(String msg) {
                    FileUtils.logTextFile(TextFileType.HISTORY, userLogin, history);
                    Log.e(TAG, "History file upload: "+msg);

                }
            };

            NetworkUtils.post(context, "/histories", history, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    hlogger.log("unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    hlogger.log("failure");
                }

                @Override
                public void noConnection() {
                    hlogger.log("no connection");
                }

                @Override
                public void badConnection() {
                    hlogger.log("bad connection");
                }

                @Override
                public void badRequest() {
                    hlogger.log("bad request");
                }

                @Override
                public void badResponse() {
                    hlogger.log("bad response");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "error sending history", e);
        }
    }
}
