package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by brunosiqueira on 13/10/15.
 */

class HeartbeatLoggedCallback extends LoggedHTTPResponseCallback {

    public HeartbeatLoggedCallback(Context context, JsonDataType jsonDataType, JSONObject data, String tag) {
        super(context, jsonDataType, data, tag);
    }

    @Override
    protected void logFailedData(String id) {
        Log.e(this.tag, "heartbeat not sent successfully: " + id);
        String userLogin = Globals.getUserLogin(this.context);
        try {
            SqliteUtils.storeToDb(this.context, userLogin, JsonDataType.TYPE_LOCATION_INFO, this.data.getJSONObject("location"));
        } catch (JSONException e) {
            ILog.e(this.tag, "heartbeat object without location information", e);
        }

        if (this.data.has("battery"))
            try {
                SqliteUtils.storeToDb(this.context, userLogin, JsonDataType.TYPE_BATTERY_STATUS, this.data.getJSONObject("battery"));
            } catch (JSONException e) {
                ILog.e(this.tag, "heartbeat with wrong battery field", e);
            }
    }
}

public class HeartBeatUtils {
    private static final String TAG = HeartBeatUtils.class.getName();

    public static void sendHeartBeat(Context context, final JSONObject locationJson, final JSONObject batteryJson){

        try {
            JSONObject json = new JSONObject();
            json.put("location", locationJson);
            if (batteryJson != null) {
                json.put("battery", batteryJson);
            }

            final LoggedHTTPResponseCallback hblogger = new HeartbeatLoggedCallback(context, JsonDataType.TYPE_HEARTBEAT_DATA, json, TAG);

            NetworkUtils.post(context, "/heartbeats", json, hblogger);
        } catch (JSONException e) {
            ILog.e(TAG, "error sending heartbeat", e);
        }

    }

}
