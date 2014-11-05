package org.igarape.copcast.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.LocationUtils;
import org.igarape.copcast.views.MainActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Created by bruno on 5/11/2014.
 */
public class LocationService extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener{
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    private int mId = 2;

    public LocationService(String name) {
        super(name);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(mId);

        if (mLocationClient.isConnected()){
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent resultIntent = new Intent(this, MainActivity.class);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notification_location_title))
                .setContentText(getString(R.string.notification_location_description))
                .setSmallIcon(R.drawable.ic_launcher);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
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


    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);


        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

        mLocationClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        LocationUtils.sendLocation(this, Globals.getUserLogin(this), location);
    }



    public static JSONObject buildJson(Location location) throws JSONException {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
        return buildJson(location.getLatitude(),location.getLongitude(),df.format(new Date()),
                location.getAccuracy(), location.getExtras() == null ? null : location.getExtras().get("satellites"), location.getProvider(),
                location.getBearing(), location.getSpeed());
    }

    public static JSONObject buildJson(Double latitude, Double longitude, String date,
                                       Float accuracy, Object satellites, String provider,
                                       Float bearing, Float speed) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("lat", latitude);
        json.put("lng", longitude);
        json.put("date", date);

        if (accuracy != null) {
            json.put("accuracy", accuracy);
        }
        if (satellites != null) {
            json.put("satellites", satellites);
        }
        if (provider != null) {
            json.put("provider", provider);
        }
        if (bearing != null) {
            json.put("bearing", bearing);
        }
        if (speed != null) {
            json.put("speed", speed);
        }
        return json;
    }


}
