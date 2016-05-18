package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.igarape.copcast.R;
import org.igarape.copcast.service.sign.SigningService;
import org.igarape.copcast.service.sign.SigningServiceException;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.OkDialog;
import org.igarape.copcast.promises.Promise;

/**
 * Created by martelli on 2/2/16.
 */
public class RegistrationActivity extends Activity {

    private String TAG = RegistrationActivity.class.getCanonicalName();
    private TextView register_url;
    private TextView register_username;
    private TextView register_password;
    private Button doRegister;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Log.e(TAG, "imei >>"+Globals.getImei());
        Log.e(TAG, "simid >>"+Globals.getSimid());

        register_url = (TextView) findViewById(R.id.txtRegisterUrl);
        register_url.setText(Globals.getServerUrl(this));
        register_username = (TextView) findViewById(R.id.txtRegisterUsername);
        register_password = (TextView) findViewById(R.id.txtRegisterPassword);
        doRegister = (Button) findViewById(R.id.btnDoRegister);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.registering));
        progressDialog.setMessage(getString(R.string.please_hold));

        doRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRegister();
            }
        });

        showKeyboard(register_url);
        showKeyboardOnFocusAndClick(register_url);
        showKeyboardOnFocusAndClick(register_username);
    }

    private void doRegister() {
        progressDialog.show();
        final String url = register_url.getText().toString();
        final String username = register_username.getText().toString();
        final String pwd = register_password.getText().toString();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    SigningService.registration(RegistrationActivity.this, url, username, pwd, new Promise() {
                        @Override
                        public void success() {
                            progressDialog.dismiss();
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(RegistrationActivity.this);
                            SharedPreferences.Editor edit = sharedPref.edit();
                            edit.putString(Globals.SERVER_URL, url);
                            edit.commit();

                            Intent toMainIntent = new Intent(RegistrationActivity.this, LoginActivity.class);
                            startActivity(toMainIntent);
                            RegistrationActivity.this.finish();
                        }

                        @Override
                        public void error(String error) {
                            progressDialog.dismiss();
                            final String reason = error;

                            try {
                                SigningService.removeKey();
                            } catch (SigningServiceException e) {
                                Log.e(TAG, "Error removing key (" + reason + ")", e);
                            }
                            OkDialog.display(RegistrationActivity.this, null, reason);
                        }
                    });
                } catch (SigningServiceException e) {
                    OkDialog.display(RegistrationActivity.this, null, getString(R.string.error_keystore));
                }
                return null;
            }
        }.execute();
    }

    private void showKeyboard(final TextView tv) {
        tv.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(tv, 0);
            }
        }, 200);
    }

    private void showKeyboardOnFocusAndClick(final TextView tv) {
        // Show the keyboard when the user first focuses in the field.
        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboard(tv);
                }
            }
        });
        // Show keyboard on click, in case the user has closed the keyboard.
        // Click does not fire when the user first focuses in the field.
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard(tv);
            }
        });
    }
}
