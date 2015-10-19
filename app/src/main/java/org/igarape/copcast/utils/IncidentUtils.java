package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class IncidentUtils {

    private static final String TAG = HistoryUtils.class.getName();

    public static JSONObject buildJson(String videoPath) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("timestamp", TimeUtils.getTimestamp().toString());
        json.put("videoPath", videoPath);
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

    public static void registerIncident(Context context, final String videoPath) {

        JSONObject incident;
        String userLogin = Globals.getUserLogin(context);

        try {
            incident = buildJson(videoPath);
            FileUtils.LogIncident(userLogin, incident);

            NetworkUtils.post(context, "/incidents", incident, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    Log.e(TAG, "incident not sent successfully");
                }

                @Override
                public void failure(int statusCode) {
                    Log.e(TAG, "incident not sent successfully");
                }

                @Override
                public void noConnection() {
                    Log.e(TAG, "incident not sent successfully");
                }

                @Override
                public void badConnection() {
                    Log.e(TAG, "incident not sent successfully");
                }

                @Override
                public void badRequest() {
                    Log.e(TAG, "incident not sent successfully");
                }

                @Override
                public void badResponse() {
                    Log.e(TAG, "incident not sent successfully");
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "error sending incident", e);
        }
    }
}
