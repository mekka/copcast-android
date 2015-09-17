package org.igarape.copcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import org.igarape.copcast.utils.BatteryUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.LocationUtils;

/**
 * Created by FCavalcanti on 7/3/15.
 */
public class AlarmLocationReceiver extends BroadcastReceiver {

    public static String TAG = AlarmLocationReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive...");
        if(BatteryUtils.shouldUpload()){
            final Location lastKnownLocation = Globals.getLastKnownLocation();
            if(null != lastKnownLocation){
                LocationUtils.sendLocation(context, Globals.getUserLogin(context),BatteryUtils.getBatteryPercentage(), lastKnownLocation);
            }else{
                Log.d(TAG, "no location found.");
            }
        }else{
            Log.d(TAG, "low battery.");
        }
    }
}
