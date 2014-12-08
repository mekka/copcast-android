package org.igarape.copcast.utils;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by bruno on 11/5/14.
 */
public class LocationUtils {
    /*
  * Define a request code to send to Google Play services
  * This code is returned in Activity.onActivityResult
  */
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /*
     * Constants for location update parameters
     */
    // Milliseconds per second
    public static final int MILLISECONDS_PER_SECOND = 1000;

    // The update interval
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    // A fast interval ceiling
    public static final int FAST_CEILING_IN_SECONDS = 1;

    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * FAST_CEILING_IN_SECONDS;
    private static final String TAG = LocationUtils.class.getName();

    public static void sendLocation(Context context, final String login, final Location location) {
        HttpResponseCallback callback = new HttpResponseCallback() {
            @Override
            public void failure(int statusCode) {
                FileUtils.logLocation(login, location);
            }

            @Override
            public void unauthorized() {
                FileUtils.logLocation(login, location);
            }

            @Override
            public void noConnection() {
                FileUtils.logLocation(login, location);
            }

            @Override
            public void badConnection() {
                FileUtils.logLocation(login, location);
            }

            @Override
            public void badRequest() {
                FileUtils.logLocation(login, location);
            }

            @Override
            public void badResponse() {
                FileUtils.logLocation(login, location);
            }

            @Override
            public void success(JSONObject response) {
                Log.i(TAG, "location sent successfully");
            }
        };

        try {
            NetworkUtils.post(context, "/locations", buildJson(location), callback);
        } catch (JSONException e) {
            Log.e(TAG, "json error", e);
        }
    }

    public static JSONObject buildJson(Location location) throws JSONException {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
        return buildJson(location.getLatitude(), location.getLongitude(), df.format(new Date()),
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
