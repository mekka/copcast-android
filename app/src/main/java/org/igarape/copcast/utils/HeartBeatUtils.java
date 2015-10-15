package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by brunosiqueira on 13/10/15.
 */
public class HeartBeatUtils {
    private static final String TAG = HeartBeatUtils.class.getName();

    public static void sendHeartBeat(Context context, final String login, final JSONObject locationJson, final JSONObject batteryJson){

        try {
            JSONObject json = new JSONObject();
            json.put("location", locationJson);
            if (batteryJson != null) {
                json.put("battery", batteryJson);
            }
            NetworkUtils.post(context, "/heartbeats", json, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    if (batteryJson != null) {
                        FileUtils.logBattery(login, batteryJson);
                    }
                    FileUtils.logLocation(login, locationJson);
                }

                @Override
                public void failure(int statusCode) {
                    if (batteryJson != null) {
                        FileUtils.logBattery(login, batteryJson);
                    }
                    FileUtils.logLocation(login, locationJson);
                }

                @Override
                public void noConnection() {
                    if (batteryJson != null) {
                        FileUtils.logBattery(login, batteryJson);
                    }
                    FileUtils.logLocation(login, locationJson);
                }

                @Override
                public void badConnection() {
                    if (batteryJson != null) {
                        FileUtils.logBattery(login, batteryJson);
                    }
                    FileUtils.logLocation(login, locationJson);
                }

                @Override
                public void badRequest() {
                    if (batteryJson != null) {
                        FileUtils.logBattery(login, batteryJson);
                    }
                    FileUtils.logLocation(login, locationJson);
                }

                @Override
                public void badResponse() {
                    if (batteryJson != null) {
                        FileUtils.logBattery(login, batteryJson);
                    }
                    FileUtils.logLocation(login, locationJson);
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "error sending heartbeat", e);
        }

    }
}
