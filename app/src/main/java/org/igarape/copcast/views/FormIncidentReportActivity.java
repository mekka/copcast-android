package org.igarape.copcast.views;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
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

    //UI Components
    TextView txtDate;
    TextView txtTime;
    TextView txtLocation;
    EditText txtAddress;

    CheckBox chkAccident;
    SeekBar skbAccGravity;
    EditText txtAccNumInjured;

    CheckBox chkFine;
    EditText txtFineType;

    CheckBox chkArrest;
    CheckBox chkArrResistance;
    CheckBox chkArrResArgument;
    CheckBox chkArrResUseForce;
    CheckBox chkArrResUseLetahlForce;


    private Button btnSendForm;
    private String TAG = FormIncidentReportActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_incident_report);

        initForm();

        btnSendForm = (Button) findViewById(R.id.btnFormSend);

        btnSendForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIndicidentForm(v);
            }
        });
    }

    private void initForm() {


        //bind the UI components
        txtDate = (TextView) findViewById(R.id.txtDate);
        txtTime = (TextView) findViewById(R.id.txtTime);
        txtLocation = (TextView) findViewById(R.id.txtLocation);
        txtAddress = (EditText)  findViewById(R.id.txtAddress);

        chkAccident = (CheckBox) findViewById(R.id.chkAccident);
        skbAccGravity = (SeekBar) findViewById(R.id.skbAccGravity);
        txtAccNumInjured = (EditText) findViewById(R.id.txtAccNumInjured);

        chkFine = (CheckBox) findViewById(R.id.chkFine);
        txtFineType = (EditText) findViewById(R.id.txtFineType);

        chkArrest = (CheckBox) findViewById(R.id.chkArrest);
        chkArrResistance = (CheckBox) findViewById(R.id.chkArrResistance);
        chkArrResArgument = (CheckBox) findViewById(R.id.chkArrResArgument);
        chkArrResUseForce = (CheckBox) findViewById(R.id.chkArrResUseForce);
        chkArrResUseLetahlForce = (CheckBox) findViewById(R.id.chkArrResUseLetahlForce);



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
