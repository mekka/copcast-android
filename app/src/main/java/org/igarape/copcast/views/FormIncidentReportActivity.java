package org.igarape.copcast.views;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.igarape.copcast.BO.IncidentForm;
import org.igarape.copcast.R;
import org.igarape.copcast.utils.BatteryUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.IncidentFormUtils;
import org.igarape.copcast.utils.LocationUtils;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormIncidentReportActivity extends Activity {

    private Button btnSendForm;
    private String TAG = FormIncidentReportActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_incident_report);

        btnSendForm = (Button) findViewById(R.id.btnFormSend);

        btnSendForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIndicidentForm(v);
            }
        });
    }

    private void sendIndicidentForm(View v) {

        IncidentForm incident ;

        incident = getIncidentForm();

        sendIncidentForm(incident);

        Toast toast= Toast.makeText(getApplicationContext(), "Form sent", Toast.LENGTH_LONG);
        toast.show();
    }

    private void sendIncidentForm(IncidentForm incidentForm) {

        Log.d(TAG, "writeJSONtoFile...");

        IncidentFormUtils.sendForm(getApplicationContext(),
                    Globals.getUserLogin(getApplicationContext()),
                    incidentForm);
    }

    private IncidentForm getIncidentForm() {

        Location lastKnownLocation = Globals.getLastKnownLocation();

        IncidentForm incidentForm = new IncidentForm();
        
        incidentForm.setDate(new Date());
        incidentForm.setLat((float) lastKnownLocation.getLatitude());
        incidentForm.setLng((float) lastKnownLocation.getLongitude());
        incidentForm.setAccident(true); // TODO: 10/22/15 get from form
        incidentForm.setGravity(5);
        incidentForm.setInjured(3);
        incidentForm.setFine(true);
        incidentForm.setFineType("speed");
        incidentForm.setArrest(true);
        incidentForm.setResistance(true);
        incidentForm.setArgument(true);
        incidentForm.setUseOfForce(true);
        incidentForm.setUseLethalForce(true);

        return incidentForm;

    }
}
