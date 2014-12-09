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
    private static final String TAG = HistoryUtils.class.getName();

    public static JSONObject buildJson(State currentState, State nextState) throws JSONException {
        JSONObject json = new JSONObject();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
        json.put("previousState", currentState.toString());
        json.put("nextState", nextState.toString());
        json.put("date", df.format(new Date()));
        return json;
    }

    public static void registerHistory(Context context, State currentState, State nextState, final String userLogin) {

        try {
            final JSONObject history = buildJson(currentState, nextState);
            NetworkUtils.post(context, "/histories", history, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    Log.e(TAG, "history not sent successfully");
                    FileUtils.LogHistory(userLogin, history);
                }

                @Override
                public void failure(int statusCode) {
                    Log.e(TAG, "history not sent successfully");
                    FileUtils.LogHistory(userLogin, history);
                }

                @Override
                public void noConnection() {
                    Log.e(TAG, "history not sent successfully");
                    FileUtils.LogHistory(userLogin, history);
                }

                @Override
                public void badConnection() {
                    Log.e(TAG, "history not sent successfully");
                    FileUtils.LogHistory(userLogin, history);
                }

                @Override
                public void badRequest() {
                    Log.e(TAG, "history not sent successfully");
                    FileUtils.LogHistory(userLogin, history);
                }

                @Override
                public void badResponse() {
                    Log.e(TAG, "history not sent successfully");
                    FileUtils.LogHistory(userLogin, history);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
