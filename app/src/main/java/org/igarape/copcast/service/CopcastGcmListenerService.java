package org.igarape.copcast.service;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by bruno on 11/18/14.
 */
public class CopcastGcmListenerService extends GcmListenerService {
    private static final String KEY_STREAMING_START = "startStreaming";
    private static final String KEY_STREAMING_STOP = "stopStreaming";
    public static final String START_STREAMING_ACTION = "org.igarape.copcast.START_STREAMING_ACTION";
    public static final String STOP_STREAMING_ACTION = "org.igarape.copcast.STOP_STREAMING_ACTION";
    public static String TAG = CopcastGcmListenerService.class.getName();



    @Override
    public void onMessageReceived(String from, Bundle data) {


        if (!data.isEmpty()) {
            String message = data.getString("message");
            Log.d(TAG, "From: " + from);
            Log.d(TAG, "Message: " + message);

            String key = data.getString("collapse_key");
            LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(getApplicationContext());
            if (KEY_STREAMING_START.equals(key)) {
                broadcaster.sendBroadcast(new Intent(START_STREAMING_ACTION));
            } else {
                broadcaster.sendBroadcast(new Intent(STOP_STREAMING_ACTION));
            }
        }
    }
}
