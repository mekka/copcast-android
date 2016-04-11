package org.igarape.copcast.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by martelli on 11/12/15.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    public static int hour, minute;
    private TextView txtview;

    public static void getCurrentTime() {
        final Calendar c = Calendar.getInstance();
        hour = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);
    }

    public static String asString() {
        getCurrentTime();
        return String.format("%02d", hour)+":"+String.format("%02d", minute);
    }

    public void setTxtView(TextView txtview) {
        this.txtview = txtview;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getCurrentTime();
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        txtview.setText(String.format("%02d", hourOfDay)+":"+String.format("%02d", minute));
    }
}