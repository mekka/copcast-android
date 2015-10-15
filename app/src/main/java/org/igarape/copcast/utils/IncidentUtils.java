package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class IncidentUtils {

    private static final String TAG = HistoryUtils.class.getName();

    public static JSONObject buildJson(String videoName) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("timestamp", TimeUtils.getTimestamp().toString());
        return json;
    }

//    public static void updateIncident(final String userLogin, String videoName) {
//        JSONArray entries = FileUtils.getContentAsJsonArray(userLogin, FileUtils.INCIDENTS_TXT);
//        try {
//            int last_pos = entries.length()-1;
//            JSONObject last_obj = entries.getJSONObject(last_pos);
//            last_obj.put("video_name", videoName);
//            entries.put(last_pos, last_obj);
//            FileUtils.setContentFromJsonArray(userLogin, FileUtils.INCIDENTS_TXT, entries);
//        } catch (JSONException e) {
//            Log.e(TAG, "error parsing incidents", e);
//        }
//    }

    public static void storeIncidentVideoName(String userLogin, String videoName) {
        FileUtils.LogIncidentVideo(userLogin, videoName);
    }

    public static void registerIncident(Context context, final String userLogin) {

        String videoName = "videoteste";
        JSONObject incident;

        try {
            incident = buildJson(videoName);
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
