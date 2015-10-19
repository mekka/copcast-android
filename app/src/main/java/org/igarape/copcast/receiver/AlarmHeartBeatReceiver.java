package org.igarape.copcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import org.igarape.copcast.utils.BatteryUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HeartBeatUtils;
import org.igarape.copcast.utils.LocationUtils;
import org.json.JSONException;

/**
 * Created by FCavalcanti on 7/3/15.
 */
public class AlarmHeartBeatReceiver extends BroadcastReceiver {

    public static String TAG = AlarmHeartBeatReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive...");
        if(BatteryUtils.shouldUpload()){
            Location lastKnownLocation = Globals.getLastKnownLocation();

            if(null != lastKnownLocation){
                try {
                    HeartBeatUtils.sendHeartBeat(context, Globals.getUserLogin(context),
                            LocationUtils.buildJson(lastKnownLocation), BatteryUtils.buildJson());
                } catch (JSONException e) {
                    Log.e(TAG, "error parsing location.", e);
                }
            }else{
                Log.d(TAG, "no location found.");
            }
        }else{
            Log.d(TAG, "low battery.");
        }
    }
}
