package org.igarape.copcast.views;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.igarape.copcast.R;
import org.igarape.copcast.bo.IncidentForm;
import org.igarape.copcast.fragments.DatePickerFragment;
import org.igarape.copcast.fragments.TimePickerFragment;
import org.igarape.copcast.utils.GPSTracker;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.IncidentFormCallback;
import org.igarape.copcast.utils.IncidentFormUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FormIncidentReportActivity extends Activity {

    //context
    Context context;

    //UI Components
    DatePicker datePicker;
    TimePicker timePicker;
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
    LinearLayout form;
    ImageButton btnSetDate;
    ImageButton btnSetTime;

    TextView txtDate;
    TextView txtTime;


    private Set<View> visitedCheckboxes;

    private Button btnSendForm;
    private String TAG = FormIncidentReportActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        Log.d(TAG, "FormIncident created");
        setContentView(R.layout.activity_form_incident_report);
        sv = (ScrollView)findViewById(R.id.scrollView1);
        form = (LinearLayout) findViewById(R.id.form);
        btnSetDate = (ImageButton) findViewById(R.id.btnSetDate);
        btnSetTime = (ImageButton) findViewById(R.id.btnSetTime);
        visitedCheckboxes = new HashSet<View>();
        initForm();

        btnSendForm = (Button) findViewById(R.id.btnFormSend);

        btnSendForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateFormBeforeSend(v)) {
                    sendIncidentForm(v);
                }
            }
        });

    }

    private void setScrollDown(final View v, boolean force) {

        if (!force && visitedCheckboxes.contains(v)) // if not forced, scrolls only at the first touch
            return;
        visitedCheckboxes.add(v);
        ViewTreeObserver vto = sv.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int bottom = form.getBottom();
                sv.scrollTo(0, bottom+20);
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
            Toast.makeText(context, getString(R.string.fill_address), Toast.LENGTH_SHORT).show();
        }

        if (chkFine.isChecked()) {
            if (txtFineType.getText().toString().length() == 0) {
                validated = false;
                Toast.makeText(context, getString(R.string.fill_fine_type), Toast.LENGTH_SHORT).show();
            }
        }
        if (chkAccident.isChecked())
        {
            if ( txtAccNumInjured.getText().toString().length() == 0) {
                validated = false;
                Toast.makeText(context, getString(R.string.fill_number_injured), Toast.LENGTH_SHORT).show();
            }
        }
        Log.d(TAG, "validateFormBeforeSend");

        return validated;
    }

    private void initForm() {

        txtTime = (TextView) findViewById(R.id.txtTime);
        txtDate = (TextView) findViewById(R.id.txtDate);

        txtDate.setText(DatePickerFragment.asString());
        txtTime.setText(TimePickerFragment.asString());

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
                txtAddress.setHint(getString(R.string.address_not_detected));
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
                setScrollDown(chkAccident, false);
                if (((CheckBox) v).isChecked()) {
                    findViewById(R.id.accidentLayout).setVisibility(View.VISIBLE);
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
                setScrollDown(chkFine, false);
                if (((CheckBox)v).isChecked()){
                    findViewById(R.id.fineLayout).setVisibility(View.VISIBLE);
//                    sv.scrollTo(0, sv.getBottom());
                    sv.fullScroll(ScrollView.FOCUS_DOWN);
                } else {
                    txtFineType.setText(null);
                    findViewById(R.id.fineLayout).setVisibility(View.GONE);
                }
            }
        });

        chkArrest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setScrollDown(chkArrest, true);
                if (((CheckBox)v).isChecked()){
                    findViewById(R.id.arrestLayout).setVisibility(View.VISIBLE);
                } else {
                    chkArrResistance.setChecked(false);
                    chkArrResArgument.setChecked(false);
                    chkArrResUseForce.setChecked(false);
                    chkArrResUseLetahlForce.setChecked(false);
                    findViewById(R.id.arrestLayout).setVisibility(View.GONE);
                    findViewById(R.id.resistanceLayout).setVisibility(View.GONE);
                }
            }
        });

        chkArrResistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setScrollDown(chkArrResistance, true);
                if (((CheckBox) v).isChecked()) {
                    findViewById(R.id.resistanceLayout).setVisibility(View.VISIBLE);
                } else {
                    chkArrResArgument.setChecked(false);
                    chkArrResUseForce.setChecked(false);
                    chkArrResUseLetahlForce.setChecked(false);
                    findViewById(R.id.resistanceLayout).setVisibility(View.GONE);
                }
            }
        });


        btnSetDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();
                ((DatePickerFragment)newFragment).setTxtView(txtDate);
                newFragment.show(getFragmentManager(), "datePicker");
            }

        });

        btnSetTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new TimePickerFragment();
                ((TimePickerFragment)newFragment).setTxtView(txtTime);
                newFragment.show(getFragmentManager(), "timePicker");
            }
        });

        //touching the gravity ticks sets its value
        int[] ticks = new int[] {R.id.gv1, R.id.gv2, R.id.gv3, R.id.gv4, R.id.gv5};
        for(int i=0; i<=4; i++) {
            final int num = i;
            int v = ticks[i];
            findViewById(v).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    skbAccGravity.setProgress(num);
                    return true;
                }
            });
        }
    }

    private void sendIncidentForm(View v) {

        IncidentForm incident;
        Drawable color;
        color = btnSendForm.getBackground();
        try {
            //change button behavior
            btnSendForm.setBackgroundColor( color.getOpacity() );
            btnSendForm.setEnabled(false);

            incident = getIncidentForm();

            sendIncidentForm(incident);

            Toast.makeText(context, getString(R.string.form_sent), Toast.LENGTH_LONG);
            btnSendForm.setBackground(color);
            btnSendForm.setEnabled(true);

            finish();
        }
        catch (Exception e)
        {
            Log.e(TAG, "error sending the form", e);
            Toast.makeText(context, getString(R.string.error_sending_form), Toast.LENGTH_LONG).show();
            btnSendForm.setBackground(color);
            btnSendForm.setEnabled(true);

        }



    }

    private void sendIncidentForm(IncidentForm incidentForm) {

        Log.d(TAG, "writeJSONtoFile...");

        IncidentFormUtils.sendForm(getApplicationContext(),
                Globals.getUserLogin(getApplicationContext()),
                incidentForm, new IncidentFormCallback() {
                    @Override
                    public void failure() {
                        Toast.makeText(context, getString(R.string.error_sending_form), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void success() {
                    }
                });
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



        try {
            DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH);
            Date date = format.parse(txtDate.getText().toString()+" "+txtTime.getText().toString());
            incidentForm.setDate(date);
        } catch (ParseException e) {
            Log.e(TAG, "error parsing date", e);
        }


        incidentForm.setLat(Float.parseFloat(strLatitude));
        incidentForm.setLng(Float.parseFloat(strLongitude));
        incidentForm.setAddress(txtAddress.getText().toString());

        incidentForm.setAccident(chkAccident.isChecked());
        incidentForm.setGravity(skbAccGravity.getProgress());
        if (chkAccident.isChecked()) {
            incidentForm.setInjured(Integer.parseInt(txtAccNumInjured.getText().toString()));
        }
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
