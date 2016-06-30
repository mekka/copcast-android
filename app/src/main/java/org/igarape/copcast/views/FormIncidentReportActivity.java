package org.igarape.copcast.views;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.igarape.copcast.R;
import org.igarape.copcast.bo.IncidentForm;
import org.igarape.copcast.fragments.DatePickerFragment;
import org.igarape.copcast.fragments.TimePickerFragment;
import org.igarape.copcast.utils.GPSTracker;
import org.igarape.copcast.utils.IncidentFormUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class FormIncidentReportActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //context
    Context context;

    //UI Components
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
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        Log.d(TAG, "FormIncident created");
        setContentView(R.layout.activity_form_incident_report);
        sv = (ScrollView) findViewById(R.id.scrollView1);
        form = (LinearLayout) findViewById(R.id.form);
        btnSetDate = (ImageButton) findViewById(R.id.btnSetDate);
        btnSetTime = (ImageButton) findViewById(R.id.btnSetTime);
        visitedCheckboxes = new HashSet<>();
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


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();

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
                sv.scrollTo(0, bottom + 20);
            }
        });
    }

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
        if (chkAccident.isChecked()) {
            if (txtAccNumInjured.getText().toString().length() == 0) {
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
        txtAddress.setHint(getString(R.string.address_not_detected));

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
                if (((CheckBox) v).isChecked()) {
                    findViewById(R.id.fineLayout).setVisibility(View.VISIBLE);
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
                if (((CheckBox) v).isChecked()) {
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
                ((DatePickerFragment) newFragment).setTxtView(txtDate);
                newFragment.show(getFragmentManager(), "datePicker");
            }

        });

        btnSetTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new TimePickerFragment();
                ((TimePickerFragment) newFragment).setTxtView(txtTime);
                newFragment.show(getFragmentManager(), "timePicker");
            }
        });

        //touching the gravity ticks sets its value
        int[] ticks = new int[]{R.id.gv1, R.id.gv2, R.id.gv3, R.id.gv4, R.id.gv5};
        for (int i = 0; i <= 4; i++) {
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
            btnSendForm.setBackgroundColor(color.getOpacity());
            btnSendForm.setEnabled(false);

            incident = getIncidentForm();

            sendIncidentForm(incident);

            Toast.makeText(context, getString(R.string.form_sent), Toast.LENGTH_LONG);
            btnSendForm.setBackground(color);
            btnSendForm.setEnabled(true);

            finish();
        } catch (Exception e) {
            Log.e(TAG, "error sending the form", e);
            Toast.makeText(context, getString(R.string.error_sending_form), Toast.LENGTH_LONG).show();
            btnSendForm.setBackground(color);
            btnSendForm.setEnabled(true);

        }

    }

    private void sendIncidentForm(IncidentForm incidentForm) {

        Log.d(TAG, "writeJSONtoFile...");

        IncidentFormUtils.sendForm(getApplicationContext(), incidentForm);
    }

    private IncidentForm getIncidentForm() {
        String strLatitude = "0.0";
        String strLongitude = "0.0";

        if (mLocation != null) {
            strLatitude = String.valueOf(mLocation.getLatitude());
            strLongitude = String.valueOf(mLocation.getLongitude());

        }
        IncidentForm incidentForm = new IncidentForm();


        try {
            DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            Date date = format.parse(txtDate.getText().toString() + " " + txtTime.getText().toString());
            incidentForm.setDate(date);
        } catch (ParseException e) {
            Log.e(TAG, "error parsing date", e);
        }


        incidentForm.setLat(Float.parseFloat(strLatitude));
        incidentForm.setLng(Float.parseFloat(strLongitude));
        incidentForm.setAddress(txtAddress.getText().toString());

        incidentForm.setAccident(chkAccident.isChecked());
        incidentForm.setGravity(skbAccGravity.getProgress() + 1);
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

    @Override
    public void onConnected(Bundle bundle) {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLocation != null) {
            Address address = GPSTracker.getGeocoderAddress(getApplicationContext(), mLocation);
            if (address != null) {
                String strLatitude = String.valueOf(mLocation.getLatitude());
                String strLongitude = String.valueOf(mLocation.getLongitude());

                StringBuilder addressString = new StringBuilder();
                if (address.getAddressLine(0) != null) {
                    addressString.append(address.getAddressLine(0));
                }
                if (address.getLocality() != null) {
                    addressString.append("\n");
                    addressString.append(address.getLocality());
                }
                if (address.getPostalCode() != null) {
                    addressString.append("\n");
                    addressString.append(address.getPostalCode());
                }
                if (address.getCountryName() != null) {
                    addressString.append("\n");
                    addressString.append(address.getCountryName());
                }

                if (addressString.length() > 0) {
                    txtAddress.setText(addressString.toString());
                }

                txtLocation.setText(strLatitude + " / " + strLongitude);
            }
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        GPSTracker.showSettingsAlert(FormIncidentReportActivity.this);
    }
}
