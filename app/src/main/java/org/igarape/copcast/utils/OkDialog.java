package org.igarape.copcast.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import org.igarape.copcast.R;

/**
 * Created by martelli on 2/3/16.
 */
public class OkDialog extends DialogFragment {

    private static String TAG = OkDialog.class.getCanonicalName();
    private String message = "";

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(this.message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    public static void display(FragmentManager fragmentManager, String message) {

        OkDialog msg = new OkDialog();
        msg.setMessage(message);
        msg.show(fragmentManager, TAG);
    }
}
