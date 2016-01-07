package org.igarape.copcast.utils;

import android.content.Context;

import org.igarape.copcast.db.JsonDataType;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by brunosiqueira on 13/10/15.
 */
public class HeartBeatUtils {
    private static final String TAG = HeartBeatUtils.class.getName();

    public static void sendHeartBeat(Context context, final JSONObject locationJson, final JSONObject batteryJson){

        try {
            JSONObject json = new JSONObject();
            json.put("location", locationJson);
            if (batteryJson != null) {
                json.put("battery", batteryJson);
            }

            final GenericSqliteLogger hblogger = new GenericSqliteLogger(context, JsonDataType.TYPE_HEARTBEAT_DATA, json, TAG);

            NetworkUtils.post(context, "/heartbeats", json, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    hblogger.logFailedData("unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    hblogger.logFailedData("failure");
                }

                @Override
                public void noConnection() {
                    hblogger.logFailedData("noConnection");
                }

                @Override
                public void badConnection() {
                    hblogger.logFailedData("badConnection");
                }

                @Override
                public void badRequest() {
                    hblogger.logFailedData("badRequest");
                }

                @Override
                public void badResponse() {
                    hblogger.logFailedData("badResponse");
                }
            });
        } catch (JSONException e) {
            ILog.e(TAG, "error sending heartbeat", e);
        }

    }

}
