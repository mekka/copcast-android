package org.igarape.copcast.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import org.igarape.copcast.R;

/**
 * Created by martelli on 2/3/16.
 */
public class OkDialog extends DialogFragment {

    private static String TAG = OkDialog.class.getCanonicalName();
    private String message = "";
    private String title = "";
    private Activity activity;
    private boolean mustTerminate = false;

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setMustTerminate(boolean t) {
        this.mustTerminate = t;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(this.message);

        if (this.title != null)
            builder.setTitle(this.title);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (OkDialog.this.mustTerminate && OkDialog.this.activity != null)
                    OkDialog.this.activity.finish();
            }
        });


        return builder.create();
    }

    public void onCancel(DialogInterface dialogInterface) {
        if (OkDialog.this.mustTerminate && OkDialog.this.activity != null)
            OkDialog.this.activity.finish();
    }


    private static void _display(final Activity activity, final String title, final String message, final boolean mustTerminate) {
        activity.runOnUiThread(new Runnable() {
            public void run() {

                OkDialog msg = new OkDialog();
                msg.setMessage(message);
                msg.setTitle(title);
                msg.setMustTerminate(mustTerminate);
                msg.setActivity(activity);
                try {
                    msg.show(activity.getFragmentManager(), TAG);
                } catch(IllegalStateException e) {
                    Log.w(TAG, "Dialog missed. Activity gone before dialog display.");
                }
            }
        });
    }

    public static void displayAndTerminate(Activity activity, String title, String message) {
        _display(activity, title, message, true);
    }

    public static void display(Activity activity, String title, String message) {
        _display(activity, title, message, false);
    }
}
