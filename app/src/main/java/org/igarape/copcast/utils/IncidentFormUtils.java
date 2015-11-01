package org.igarape.copcast.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.igarape.copcast.BO.IncidentForm;
import org.igarape.copcast.db.JsonDataType;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by alexsalgado on 21/10/2015.
 */
public class IncidentFormUtils {
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";

    private static AtomicBoolean failureLogged = new AtomicBoolean(false);

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
    public static final int FAST_CEILING_IN_SECONDS = 5;

    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * FAST_CEILING_IN_SECONDS;
    private static final String TAG = IncidentFormUtils.class.getName();
    public static final float SMALLEST_DISPLACEMENT = 0;

    public static void sendForm(final Context context, final String login, final IncidentForm incidentForm) {
        HttpResponseCallback callback = new HttpResponseCallback() {
            @Override
            public void failure(int statusCode) {
                failedRegisterIncident(context, incidentForm, "failure");
            }

            @Override
            public void unauthorized() {

                failedRegisterIncident(context, incidentForm, "unauthorized");

            }

            @Override
            public void noConnection() {
                failedRegisterIncident(context, incidentForm, "noConnection");
            }

            @Override
            public void badConnection() {
                failedRegisterIncident(context, incidentForm, "badConnection");
            }

            @Override
            public void badRequest() {
                failedRegisterIncident(context, incidentForm, "badRequest");
            }

            @Override
            public void badResponse() {
                failedRegisterIncident(context, incidentForm, "badResponse");
            }

            @Override
            public void success(JSONObject response) {
                Log.i(TAG, "SendForm sent successfully");
            }
        };

        try {
            NetworkUtils.post(context, "/incidentForms", buildJson(incidentForm), callback);
        } catch (JSONException e) {
            Log.e(TAG, "json error", e);
        }
    }

    private static void failedRegisterIncident(Context context, IncidentForm incidentForm, String tag) {
        Log.e(TAG, "Formincident not sent successfully: " + tag);

        if (!failureLogged.getAndSet(true)) {
            String userLogin = Globals.getUserLogin(context);
            try {
                SqliteUtils.storeToDb(context, userLogin,
                        JsonDataType.TYPE_INCIDENT_FORM,
                        buildJson(incidentForm));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static JSONObject buildJson(IncidentForm incidentForm) throws JSONException {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
        return buildJson(new Date(),
                incidentForm.getLat(),
                incidentForm.getLng(),
                incidentForm.isAccident(),
                incidentForm.getGravity(),
                incidentForm.getInjured(),
                incidentForm.isFine(),
                incidentForm.getFineType(),
                incidentForm.isArrest(),
                incidentForm.isResistance(),
                incidentForm.isArgument(),
                incidentForm.isUseOfForce(),
                incidentForm.isUseLethalForce(),
                incidentForm.getUserId() );

    }

    public static JSONObject buildJson( Date date,
                                        float lat,
                                        float lng,
                                        boolean accident,
                                        int gravity,
                                        int injured,
                                        boolean fine,
                                        String fineType,
                                        boolean arrest,
                                        boolean resistance,
                                        boolean argument,
                                        boolean useOfForce,
                                        boolean useLethalForce,
                                        long userId) throws JSONException {

        JSONObject json = new JSONObject();

        json.put("date", date);
        json.put("lat", lat);
        json.put("lng", lng);
        json.put("accident", accident);
        json.put("gravity", gravity);
        json.put("injured", injured);
        json.put("fine", fine);
        json.put("fineType", fineType);
        json.put("arrest", arrest);
        json.put("resistance", resistance);
        json.put("argument", argument);
        json.put("useOfForce", useOfForce);
        json.put("useLethalForce", useLethalForce);
        json.put("userId", userId);


        return json;
    }

    //toast wrapper
    public static void fToast ( final  String message, final Context context)
    {
        //avoid ui thread nonsense

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                }
        );

    }
}
