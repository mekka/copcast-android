package org.igarape.copcast.views;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.igarape.copcast.bo.IncidentForm;
import org.igarape.copcast.R;
import org.igarape.copcast.fragments.DatePickerFragment;
import org.igarape.copcast.fragments.TimePickerFragment;
import org.igarape.copcast.utils.GPSTracker;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.IncidentFormUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class FormIncidentReportActivity extends Activity {

    //context
    Context c;

    //UI Components
    DatePicker datePicker;
    TimePicker timePicker;
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
    ScrollView sv;
    ImageButton btnSetDate;
    ImageButton btnSetTime;


    private Button btnSendForm;
    private String TAG = FormIncidentReportActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        c = this;
        Log.d(TAG, "FormIncident created");
        setContentView(R.layout.activity_form_incident_report);
        sv = (ScrollView)findViewById(R.id.scrollView1);
        btnSetDate = (ImageButton) findViewById(R.id.btnSetDate);
        btnSetTime = (ImageButton) findViewById(R.id.btnSetTime);

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

//    public void onClickTypeViolation(View view) {
//        Log.d(TAG, "onClickTypeViolation");
//        // Is the view now checked?
//        boolean checked = ((CheckBox) view).isChecked();
//
//        // Check which checkbox was clicked
//        switch (view.getId()) {
//            case R.id.chkAccident:
//                //if (checked) {
//                //shows Gravity and Injured
//                skbAccGravity.setEnabled(checked);
//                txtAccNumInjured.setEnabled(checked);
//
//                break;
//            case R.id.chkFine:
//                //shows/hide
//                txtFineType.setEnabled(checked);
//                break;
//            case R.id.chkArrest:
//                //shows/hide
//                chkArrResistance.setEnabled(checked);
//                chkArrResArgument.setEnabled(checked);
//                chkArrResUseForce.setEnabled(checked);
//                chkArrResUseLetahlForce.setEnabled(checked);
//
//                break;
//
//            case R.id.chkArrResistance:
//                //shows/hide
//                chkArrResArgument.setEnabled(checked);
//                chkArrResUseForce.setEnabled(checked);
//                chkArrResUseLetahlForce.setEnabled(checked);
//
//                break;
//        }
//
//
//    }

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
//        datePicker = (DatePicker) findViewById(R.id.datePicker);
//        timePicker = (TimePicker) findViewById(R.id.timePicker);

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

//        //init date and time
//        TimeZone tz = TimeZone.getTimeZone("UTC");
//        DateFormat df = new SimpleDateFormat(IncidentFormUtils.DATETIME_FORMAT);
//        df.setTimeZone(tz);
//        datePicker.setText(df.format(new Date()));

        // check if GPS enabled
        GPSTracker gpsTracker = new GPSTracker(this);

        if (gpsTracker.getIsGPSTrackingEnabled()) {
            String strLatitude = String.valueOf(gpsTracker.getLatitude());
            String strLongitude = String.valueOf(gpsTracker.getLongitude());

            txtLocation.setText(strLatitude + "/" + strLongitude);
            StringBuffer address = new StringBuffer();
            if (gpsTracker.getAddressLine(this) != null){
                address.append(gpsTracker.getAddressLine(this));
            }
            if (gpsTracker.getLocality(this) != null){
                address.append("\n");
                address.append(gpsTracker.getLocality(this));
            }
            if (gpsTracker.getPostalCode(this) != null){
                address.append("\n");
                address.append(gpsTracker.getPostalCode(this));
            }
            if (gpsTracker.getCountryName(this) != null){
                address.append("\n");
                address.append(gpsTracker.getCountryName(this));
            }

            if (address.length() == 0)
            {
                txtAddress.setText("(Address not detect automatically, please type yourself...)");
            }
            else {
                txtAddress.setText(address.toString());
            }

        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert();
        }



        chkAccident.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    findViewById(R.id.accidentLayout).setVisibility(View.VISIBLE);
                    sv.scrollTo(0, sv.getBottom());
                } else {
                    skbAccGravity.setProgress(0);
                    txtAccNumInjured.setText(null);
                    findViewById(R.id.accidentLayout).setVisibility(View.GONE);
                }
            }
        });

        chkFine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox)v).isChecked()){
                    findViewById(R.id.fineLayout).setVisibility(View.VISIBLE);
                    sv.scrollTo(0, sv.getBottom());
                } else {
                    txtFineType.setText(null);
                    findViewById(R.id.fineLayout).setVisibility(View.GONE);
                }
            }
        });

        chkArrest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox)v).isChecked()){
                    findViewById(R.id.arrestLayout).setVisibility(View.VISIBLE);
                    sv.scrollTo(0, sv.getBottom());
                } else {
                    chkArrResistance.setChecked(false);
                    chkArrResArgument.setChecked(false);
                    chkArrResUseForce.setChecked(false);
                    chkArrResUseLetahlForce.setChecked(false);
                    findViewById(R.id.arrestLayout).setVisibility(View.GONE);
                }
            }
        });

        chkArrResistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    findViewById(R.id.resistanceLayout).setVisibility(View.VISIBLE);
                    sv.scrollTo(0, sv.getBottom());
                } else {
                    chkArrResArgument.setChecked(false);
                    chkArrResUseForce.setChecked(false);
                    chkArrResUseLetahlForce.setChecked(false);
                    findViewById(R.id.resistanceLayout).setVisibility(View.GONE);
                }
            }
        });


        btnSetDate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getFragmentManager(), "datePicker");
                return true;
            }
        });

        btnSetTime.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DialogFragment newFragment = new TimePickerFragment();
                newFragment.show(getFragmentManager(), "timePicker");
                return true;
            }
        });

    }

    private void sendIndicidentForm(View v) {

        IncidentForm incident;
        Drawable color;
        color = btnSendForm.getBackground();
        try {
            //change button behavior
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

        incidentForm.setDate(getDateFromPicker(datePicker, timePicker));
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


    public static java.util.Date getDateFromPicker(DatePicker datePicker, TimePicker timePicker){
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                timePicker.getCurrentHour(), timePicker.getCurrentMinute());

        return calendar.getTime();
    }


}
