package org.igarape.copcast.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.BatteryUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HeartBeatUtils;
import org.igarape.copcast.utils.LocationUtils;
import org.igarape.copcast.views.MainActivity;
import org.json.JSONException;


/**
 * Created by bruno on 5/11/2014.
 */
public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = LocationService.class.getName();
    private int mId = 2;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(mId);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void sendHeartbeat(Location location) throws JSONException {
        Globals.setLastKnownLocation(location);
        HeartBeatUtils.sendHeartBeat(LocationService.this, LocationUtils.buildJson(location), BatteryUtils.buildJson());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location info received");
        if(null != location){
            try {
                sendHeartbeat(location);
            } catch (JSONException e) {
                Log.e(TAG, "error parsing location.", e);
            }
        }else{
            Log.d(TAG, "no location found.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){
            stopSelf();
            return START_STICKY;
        }

        final Intent resultIntent = new Intent(this, MainActivity.class);
        final Context context = getApplicationContext();


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(getString(R.string.notification_location_title))
                .setContentText(getString(R.string.notification_location_description))
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_NO_CREATE
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApiIfAvailable(LocationServices.API).build();
        mGoogleApiClient.connect();

        Log.d(TAG, "Location service started");

        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {

        // set and send last known location right before setting up request service.
        try {
            sendHeartbeat(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
        } catch(JSONException ex) {
            Log.e(TAG, "Tried to send initial location, but got null instead.");
        } catch(Exception ex) {
            Log.e(TAG, "No location information to send.");
        }

        LocationRequest mLocationRequest = new LocationRequest();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        mLocationRequest.setSmallestDisplacement(LocationUtils.SMALLEST_DISPLACEMENT);


        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

        Log.d(TAG, "Location service connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "couldn't connect to google play services");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "couldn't connect to google play services");
    }
}
