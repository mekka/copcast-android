package org.igarape.copcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.igarape.copcast.utils.BatteryUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.LocationUtils;

import java.util.List;

/**
 * Created by FCavalcanti on 7/3/15.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static String TAG = AlarmReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive...");
        if(BatteryUtils.getSingletonInstance().shouldUpload()){
            final Location lastKnownLocation = Globals.getLastKnownLocation();
            if(null != lastKnownLocation){
                LocationUtils.sendLocation(context, Globals.getUserLogin(context), lastKnownLocation);
            }else{
                Log.d(TAG, "no location found.");
            }
        }else{
            Log.d(TAG, "low battery.");
        }
    }
}
