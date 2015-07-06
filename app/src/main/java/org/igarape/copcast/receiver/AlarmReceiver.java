package org.igarape.copcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.LocationUtils;

/**
 * Created by FCavalcanti on 7/3/15.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static String TAG = AlarmReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive...");
        LocationUtils.sendLocation(context, Globals.getUserLogin(context), Globals.getLastKnownLocation());
    }
}
