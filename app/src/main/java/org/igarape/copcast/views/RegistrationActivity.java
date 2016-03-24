package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.igarape.copcast.R;
import org.igarape.copcast.exceptions.HttpPostError;
import org.igarape.copcast.service.sign.SigningService;
import org.igarape.copcast.service.sign.SigningServiceException;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.OkDialog;
import org.igarape.copcast.utils.Promise;

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


        register_url = (TextView) findViewById(R.id.txtRegisterUrl);
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
                    SigningService.registration(RegistrationActivity.this, url, username, pwd, new Promise<HttpPostError>() {
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
}
