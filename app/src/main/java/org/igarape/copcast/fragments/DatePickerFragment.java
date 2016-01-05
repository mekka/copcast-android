package org.igarape.copcast.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by martelli on 11/12/15.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    public static int year, month, day;
    private TextView txtview;

    public static void getCurrentDate() {
        final Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);
    }

    public static String asString() {
        getCurrentDate();
        return year+"/"+(month+1)+"/"+day;
    }

    public void setTxtView(TextView view) {
        this.txtview = view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getCurrentDate();
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        txtview.setText(year+"/"+(monthOfYear+1)+"/"+dayOfMonth);
    }
}