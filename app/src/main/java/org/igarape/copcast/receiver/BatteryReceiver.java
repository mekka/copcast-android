package org.igarape.copcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.igarape.copcast.utils.BatteryUtils;

/**
 * Created by FCavalcanti on 7/17/15.
 */
public class BatteryReceiver extends BroadcastReceiver {

    private static final String TAG = BatteryReceiver.class.getName();

    public static final String BATTERY_LOW_MESSAGE = "org.igarape.copcast.BATTERY_LOW";
    public static final String BATTERY_OKAY_MESSAGE = "org.igarape.copcast.BATTERY_OKAY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BATTERY_LOW.equalsIgnoreCase(intent.getAction())){
            broadcastBatteryStatus(context, BATTERY_LOW_MESSAGE);
            BatteryUtils.updateValues(intent);
        }else if(Intent.ACTION_BATTERY_OKAY.equalsIgnoreCase(intent.getAction())){
            broadcastBatteryStatus(context, BATTERY_OKAY_MESSAGE);
            BatteryUtils.updateValues(intent);
        } else {
            //Started from the AlarmReceiver
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            BatteryUtils.updateValues(context.registerReceiver(null, iFilter));
        }
    }

    private void broadcastBatteryStatus(Context context, String msg){
        Log.d(TAG, "broadcastBatteryStatus msg=["+msg+"]");
        Intent intent = new Intent(msg);
        context.sendBroadcast(intent);
    }
}
