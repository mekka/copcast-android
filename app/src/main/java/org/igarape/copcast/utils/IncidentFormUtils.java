package org.igarape.copcast.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.igarape.copcast.bo.IncidentForm;
import org.igarape.copcast.db.JsonDataType;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;


public class IncidentFormUtils {
    private static final String TAG = IncidentFormUtils.class.getName();

    public static void sendForm(final Context context, final IncidentForm incidentForm) {

        final JSONObject incidentFormJSON;

        try {
            incidentFormJSON = buildJson(incidentForm);

            final LoggedHTTPResponseCallback hlogger = new LoggedHTTPResponseCallback(context, JsonDataType.TYPE_INCIDENT_FORM, incidentFormJSON, TAG);

            NetworkUtils.post(context, JsonDataType.TYPE_INCIDENT_FORM.getUrl(), incidentFormJSON, hlogger);

        } catch (JSONException e) {
            Log.e(TAG, "error sending incidentForm.");
            return;
        }
    }

    public static JSONObject buildJson(IncidentForm incidentForm) throws JSONException {

        Log.d(TAG, "buldJson");

        return buildJson( incidentForm.getDate(),
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
                incidentForm.getUserId(),
                incidentForm.getAddress());

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
                                        long userId,
                                        String address) throws JSONException {

        JSONObject json = new JSONObject();

        SimpleDateFormat df;
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);

        json.put("date", df.format(date));
        json.put("address", address);
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
}
