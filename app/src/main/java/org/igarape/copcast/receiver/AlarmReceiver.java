package org.igarape.copcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.LocationUtils;

import java.util.List;

/**
 * Created by FCavalcanti on 7/3/15.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static String TAG = AlarmReceiver.class.getName();

    private LocationManager locationManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive...");
        final Location lastKnownLocation = Globals.getLastKnownLocation();
        if(null != lastKnownLocation){
            LocationUtils.sendLocation(context, Globals.getUserLogin(context), lastKnownLocation);
        }else{
            final Location lastSystemLocation = getLastKnownLocation(context);
            if(null != lastSystemLocation){
                LocationUtils.sendLocation(context, Globals.getUserLogin(context), lastSystemLocation);
            }else{
                Log.d(TAG, "no location found.");
            }
        }
    }

    private Location getLastKnownLocation(Context context) {
        locationManager = (LocationManager)context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }
}
