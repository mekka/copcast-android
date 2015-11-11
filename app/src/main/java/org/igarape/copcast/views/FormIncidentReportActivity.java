package org.igarape.copcast.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.igarape.copcast.bo.IncidentForm;
import org.igarape.copcast.R;
import org.igarape.copcast.utils.GPSTracker;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.IncidentFormUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class FormIncidentReportActivity extends Activity {

    //context
    Context c;

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

        c = this;
        Log.d(TAG, "FormIncident created");
        setContentView(R.layout.activity_form_incident_report);


        initForm();

        btnSendForm = (Button) findViewById(R.id.btnFormSend);

        btnSendForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateFormBeforeSend(v)) {
                    sendIndicidentForm(v);
                }
            }
        });
    }

    public void onClickTypeViolation(View view) {
        Log.d(TAG, "onClickTypeViolation");
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch (view.getId()) {
            case R.id.chkAccident:
                //if (checked) {
                //shows Gravity and Injured
                skbAccGravity.setEnabled(checked);
                txtAccNumInjured.setEnabled(checked);

                break;
            case R.id.chkFine:
                //shows/hide
                txtFineType.setEnabled(checked);
                break;
            case R.id.chkArrest:
                //shows/hide
                chkArrResistance.setEnabled(checked);
                chkArrResArgument.setEnabled(checked);
                chkArrResUseForce.setEnabled(checked);
                chkArrResUseLetahlForce.setEnabled(checked);

                break;

            case R.id.chkArrResistance:
                //shows/hide
                chkArrResArgument.setEnabled(checked);
                chkArrResUseForce.setEnabled(checked);
                chkArrResUseLetahlForce.setEnabled(checked);

                break;
        }


    }

    private boolean validateFormBeforeSend(View v) {

        boolean validated = true;

        String strAddress = txtAddress.getText().toString();
        if (strAddress.length() == 0) {
            validated = false;
            Toast.makeText(c, "Please, fill the address.", Toast.LENGTH_SHORT).show();
        }

        if (txtFineType.getText().toString().length() == 0) {
            validated = false;
            Toast.makeText(c, "Please, fill the Fine type.", Toast.LENGTH_SHORT).show();
        }

        if (chkAccident.isChecked())
        {
            if ( txtAccNumInjured.getText().toString().length() == 0) {
                validated = false;
                Toast.makeText(c, "Please, fill Number of Injured.", Toast.LENGTH_SHORT).show();
            }
        }
        Log.d(TAG, "validateFormBeforeSend");

        return validated;
    }

    private void initForm() {


        //bind the UI components
        txtDate = (TextView) findViewById(R.id.txtDate);

        txtLocation = (TextView) findViewById(R.id.txtLocation);
        txtAddress = (EditText) findViewById(R.id.txtAddress);

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

        //init date and time
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat(IncidentFormUtils.DATETIME_FORMAT);
        df.setTimeZone(tz);
        txtDate.setText(df.format(new Date()));

        // check if GPS enabled
        GPSTracker gpsTracker = new GPSTracker(this);

        if (gpsTracker.getIsGPSTrackingEnabled()) {
            String strLatitude = String.valueOf(gpsTracker.getLatitude());
            String strLongitude = String.valueOf(gpsTracker.getLongitude());

            txtLocation.setText(strLatitude + "/" + strLongitude);

            String country = gpsTracker.getCountryName(this);
            String city = gpsTracker.getLocality(this);
            String postalCode = gpsTracker.getPostalCode(this);
            String addressLine = gpsTracker.getAddressLine(this);

            if (addressLine == null)
            {
                txtAddress.setText("(Address not detect automatically, please type yourself...)");
            }
            else {
                txtAddress.setText(addressLine + "\n" +
                        city + "\n" +
                        country + "\n" +
                        postalCode);
            }

        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert();
        }

        boolean checked = false;
        //init checkBox
        skbAccGravity.setEnabled(checked);
        txtAccNumInjured.setEnabled(checked);

        //shows/hide
        txtFineType.setEnabled(checked);

        //hide
        chkArrResistance.setEnabled(checked);

        //hide
        chkArrResArgument.setEnabled(checked);
        chkArrResUseForce.setEnabled(checked);
        chkArrResUseLetahlForce.setEnabled(checked);


    }

    private void sendIndicidentForm(View v) {

        IncidentForm incident;
        Drawable color;
        color = btnSendForm.getBackground();
        try {
            //change butto behavior
            btnSendForm.setBackgroundColor( color.getOpacity() );
            btnSendForm.setEnabled(false);

            incident = getIncidentForm();

            sendIncidentForm(incident);

            Toast.makeText(c, "Form sent", Toast.LENGTH_LONG);
            btnSendForm.setBackground(color);
            btnSendForm.setEnabled(true);

            finish();
        }
        catch (Exception e)
        {
            Toast.makeText(c, "Error sending form to server, please retry!", Toast.LENGTH_LONG);
            btnSendForm.setBackground(color);
            btnSendForm.setEnabled(true);

        }



    }

    private void sendIncidentForm(IncidentForm incidentForm) {

        Log.d(TAG, "writeJSONtoFile...");

        IncidentFormUtils.sendForm(getApplicationContext(),
                Globals.getUserLogin(getApplicationContext()),
                incidentForm);
    }

    private IncidentForm getIncidentForm() {

        // check if GPS enabled
        GPSTracker gpsTracker = new GPSTracker(this);
        String address="";
        String strLatitude = "0.0";
        String strLongitude = "0.0" ;



        if (gpsTracker.getIsGPSTrackingEnabled()) {
            strLatitude = String.valueOf(gpsTracker.getLatitude());
            strLongitude = String.valueOf(gpsTracker.getLongitude());

        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert();
        }

        IncidentForm incidentForm = new IncidentForm();

        incidentForm.setDate(new Date());
        incidentForm.setLat(Float.parseFloat(strLatitude));
        incidentForm.setLng(Float.parseFloat(strLongitude));
        incidentForm.setAddress(txtAddress.getText().toString());

        incidentForm.setAccident(chkAccident.isChecked());
        incidentForm.setGravity(skbAccGravity.getProgress());
        incidentForm.setInjured(Integer.parseInt(txtAccNumInjured.getText().toString()));
        incidentForm.setFine(chkFine.isChecked());
        incidentForm.setFineType(txtFineType.getText().toString());
        incidentForm.setArrest(chkArrest.isChecked());
        incidentForm.setResistance(chkArrResistance.isChecked());
        incidentForm.setArgument(chkArrResArgument.isChecked());
        incidentForm.setUseOfForce(chkArrResUseForce.isChecked());
        incidentForm.setUseLethalForce(chkArrResUseLetahlForce.isChecked());

        return incidentForm;

    }



}