package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.igarape.copcast.R;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HistoryUtils;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.igarape.copcast.utils.NetworkUtils.post;

public class LoginActivity extends Activity {

    public static String TAG = LoginActivity.class.getName();
    EditText txtId;
    EditText txtPwd;
    ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtId = (EditText) findViewById(R.id.txtLoginUser);
        //txtId.setText(Globals.getUserLogin(this));
        txtPwd = (EditText) findViewById(R.id.txtLoginPassword);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void makeLoginRequest(View view) {
        pDialog = ProgressDialog.show(this, getString(R.string.login_in), getString(R.string.please_hold), true);

        final String regId = Globals.getRegistrationId(getApplicationContext());
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", txtId.getText().toString()));
        params.add(new BasicNameValuePair("password", txtPwd.getText().toString()));
        params.add(new BasicNameValuePair("scope", "client"));
        params.add(new BasicNameValuePair("gcm_registration", regId));

        post(this, "/token", params, new HttpResponseCallback() {
            @Override
            public void success(JSONObject response) {
                Log.d(TAG, "@JSONRESPONSE=[" + response + "]");
                String token = null;
                try {
                    token = (String) response.get("token");
                    String ipAddress = (String) response.get("ipAddress");
                    if (ipAddress != null) {
                        Globals.setServerIpAddress(getApplicationContext(),ipAddress);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "error on login", e);
                }

                try {
                    Globals.setStreamingPort(getApplicationContext(),Integer.parseInt((String) response.get("streamingPort")));
                    Globals.setStreamingUser(getApplicationContext(), (String) response.get("streamingUser"));
                    Globals.setStreamingPassword(getApplicationContext(), (String) response.get("streamingPassword"));
                    Globals.setStreamingPath(getApplicationContext(), (String) response.get("streamingPath"));
                    Globals.setUserName(getApplicationContext(), (String) response.get("userName"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (pDialog != null) {
                    pDialog.dismiss();
                    pDialog = null;
                }
                Globals.setAccessToken(getBaseContext(), token);
                Globals.setUserLogin(getBaseContext(), txtId.getText().toString());

                HistoryUtils.registerHistory(getApplicationContext(), State.NOT_LOGGED, State.LOGGED, Globals.getUserLogin(LoginActivity.this));

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
