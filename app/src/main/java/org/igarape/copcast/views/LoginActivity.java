package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.igarape.copcast.R;
import org.igarape.copcast.service.sign.SigningService;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HistoryUtils;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.view.Gravity.CENTER_HORIZONTAL;
import static org.igarape.copcast.utils.NetworkUtils.post;

public class LoginActivity extends Activity {

    public static String TAG = LoginActivity.class.getName();
    EditText txtId;
    EditText txtPwd;
    ProgressDialog pDialog;
    private Button btnLoginOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SigningService.check(this);

        Log.d(TAG, ">>>"+SigningService.signed("BOGUS"));

        txtId = (EditText) findViewById(R.id.txtLoginUser);
        txtPwd = (EditText) findViewById(R.id.txtLoginPassword);
        btnLoginOk = (Button) findViewById(R.id.btn_login_ok);
        /**
         * Appears a hack
         * On login_activity I added
         * android:focusable="true"
         * android:focusableInTouchMode="true"
         */
        txtId.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    txtId.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager keyboard = (InputMethodManager)
                                    getSystemService(Context.INPUT_METHOD_SERVICE);
                            keyboard.showSoftInput(txtId, 0);
                        }
                    }, 200);
                }
            }
        });

        btnLoginOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeLoginRequest(v);
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String msg =extras.getString("reason");
            if (msg != null && msg.length() > 0) {
                Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | CENTER_HORIZONTAL, 0, 100);
                toast.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
        }

        return true;
    }

    public void makeLoginRequest(View view) {

        pDialog = new ProgressDialog(this, android.R.style.Theme_Holo_Dialog);
        pDialog.setTitle(getString(R.string.login_in));
        pDialog.setMessage(getString(R.string.please_hold));
        pDialog.setIndeterminate(true);
        pDialog.show();

        final String loginField = txtId.getText().toString().trim();
        final String passwordField = txtPwd.getText().toString();

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] args) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();

                params.add(new BasicNameValuePair("username", loginField));

                params.add(new BasicNameValuePair("password", passwordField));
                params.add(new BasicNameValuePair("scope", "client"));

                InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
                String regId = null;

                try {
                    Bundle bundle = new Bundle();
                    bundle.putString("login", loginField);
                    regId = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, bundle);
                    Globals.storeRegistrationId(getApplicationContext(), regId);
                } catch (IOException e) {
                    Log.e(TAG, "error getting gcm code ", e);
                }
                params.add(new BasicNameValuePair("gcm_registration", regId));

                post(getApplicationContext(), "/token", params, new HttpResponseCallback() {
                    @Override
                    public void success(JSONObject response) {
                        Log.d(TAG, "@JSONRESPONSE=[" + response + "]");
                        String token = null;
                        try {
                            token = (String) response.get("token");
                            Globals.setUserName(getApplicationContext(), (String) response.get("userName"));
                        } catch (JSONException e) {
                            Log.e(TAG, "error on login", e);
                        }
                        if (pDialog != null) {
                            pDialog.dismiss();
                            pDialog = null;
                        }
                        Globals.setAccessToken(getBaseContext(), token);
                        Globals.setUserLogin(getBaseContext(), loginField);

                        HistoryUtils.registerHistory(getApplicationContext(), State.NOT_LOGGED, State.LOGGED);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        LoginActivity.this.finish();
                    }

                    @Override
                    public void unauthorized() {
                        showToast(R.string.unauthorized_login);
                    }

                    private void showToast(final int message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (pDialog != null) {
                                    pDialog.dismiss();
                                    pDialog = null;
                                }
                                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.TOP, 0, 100);
                                toast.show();

                            }
                        });
                    }

                    @Override
                    public void failure(int statusCode) {
                        showToast(R.string.server_error);
                    }

                    @Override
                    public void noConnection() {
                        showToast(R.string.network_required);
                    }

                    @Override
                    public void badConnection() {
                        showToast(R.string.connection_error);
                    }

                    @Override
                    public void badRequest() {
                        showToast(R.string.bad_request_error);
                    }

                    @Override
                    public void badResponse() {
                        showToast(R.string.bad_request_error);
                    }
                });
                return null;
            }

        }.execute(null, null, null);
    }

    private boolean hasErrors() {
        final String login = txtId.getText().toString();
        final String password = txtPwd.getText().toString();
        if (null == login || login.isEmpty()) {
            Log.d(TAG, "login required");
            Toast toast = Toast.makeText(getApplicationContext(), R.string.login_required, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 100);
            toast.show();
            return true;
        }
        if (null == password || password.isEmpty()) {
            Log.d(TAG, "password required");
            Toast toast = Toast.makeText(getApplicationContext(), R.string.password_required, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 100);
            toast.show();
            return true;
        }
        return false;
    }

}
