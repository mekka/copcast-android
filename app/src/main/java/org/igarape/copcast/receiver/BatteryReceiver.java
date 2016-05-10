package org.igarape.copcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.igarape.copcast.utils.BatteryUtils;

/**
 * Created by FCavalcanti on 7/17/15.
 */
public class BatteryReceiver extends BroadcastReceiver {

    private static final String TAG = BatteryReceiver.class.getName();

    public static final String BATTERY_LOW_MESSAGE = "org.igarape.copcast.BATTERY_LOW";
    public static final String BATTERY_OKAY_MESSAGE = "org.igarape.copcast.BATTERY_OKAY";
    public static final String POWER_UNPLUGGED = "org.igarape.copcast.POWER_UNPLUGGED";
    public static final String POWER_PLUGGED = "org.igarape.copcast.POWER_PLUGGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_LOW.equalsIgnoreCase(intent.getAction())){
            broadcastBatteryStatus(context, BATTERY_LOW_MESSAGE);
            BatteryUtils.updateValues(intent);
        } else if (Intent.ACTION_BATTERY_OKAY.equalsIgnoreCase(intent.getAction())){
            broadcastBatteryStatus(context, BATTERY_OKAY_MESSAGE);
            BatteryUtils.updateValues(intent);
        } else if (Intent.ACTION_POWER_CONNECTED.equalsIgnoreCase(intent.getAction())){
            broadcastBatteryStatus(context, POWER_PLUGGED);
        } else if (Intent.ACTION_POWER_DISCONNECTED.equalsIgnoreCase(intent.getAction())){
            broadcastBatteryStatus(context, POWER_UNPLUGGED);
        } else {
            //Started from the AlarmReceiver
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            BatteryUtils.updateValues(context.registerReceiver(null, iFilter));
        }
    }

    private void broadcastBatteryStatus(Context context, String msg){
        Log.d(TAG, "broadcastBatteryStatus msg=["+msg+"]");
        Intent intent = new Intent(msg);
        LocalBroadcastManager b = LocalBroadcastManager.getInstance(context);
        b.sendBroadcast(intent);
        //context.sendBroadcast(intent);
    }
}
