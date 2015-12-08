package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by brunosiqueira on 13/10/15.
 */
public class HeartBeatUtils {
    private static final String TAG = HeartBeatUtils.class.getName();

    interface HBLogger {
        public void log();
    }

    public static void sendHeartBeat(Context context, final String login, final JSONObject locationJson, final JSONObject batteryJson){

        final HBLogger hblogger = new HBLogger() {
            public void log() {
                if (batteryJson != null) {
                    FileUtils.logTextFile(TextFileType.BATTERY, login, batteryJson);
                }
                FileUtils.logTextFile(TextFileType.LOCATIONS, login, locationJson);
            }
        };

        try {
            JSONObject json = new JSONObject();
            json.put("location", locationJson);
            if (batteryJson != null) {
                json.put("battery", batteryJson);
            }
            NetworkUtils.post(context, "/heartbeats", json, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    hblogger.log();
                }


                @Override
                public void failure(int statusCode) {
                    hblogger.log();
                }

                @Override
                public void noConnection() {
                    hblogger.log();
                }

                @Override
                public void badConnection() {
                    hblogger.log();
                }

                @Override
                public void badRequest() {
                    hblogger.log();
                }

                @Override
                public void badResponse() {
                    hblogger.log();
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "error sending heartbeat", e);
        }

    }

}
