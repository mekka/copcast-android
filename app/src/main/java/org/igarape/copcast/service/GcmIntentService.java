package org.igarape.copcast.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.igarape.copcast.receiver.GcmBroadcastReceiver;

/**
 * Created by bruno on 11/18/14.
 */
public class GcmIntentService  extends IntentService {
    private static final String KEY_STREAMING_START = "startStreaming";
    private static final String KEY_STREAMING_STOP = "stopStreaming";
    public static final String START_STREAMING_ACTION = "org.igarape.copcast.START_STREAMING_ACTION";
    public static final String STOP_STREAMING_ACTION = "org.igarape.copcast.STOP_STREAMING_ACTION";
    public static String TAG = GcmIntentService.class.getName();

    public GcmIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {

            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                //sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                //sendNotification("Deleted messages on server: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Log.d(TAG, extras.toString());

                String key = extras.getString("collapse_key");
                LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(this);
                if (KEY_STREAMING_START.equals(key)) {
                    broadcaster.sendBroadcast(new Intent(START_STREAMING_ACTION));
                } else {
                    broadcaster.sendBroadcast(new Intent(STOP_STREAMING_ACTION));
                }
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}
