package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.igarape.copcast.R;
import org.igarape.copcast.promises.HttpPromiseError;
import org.igarape.copcast.promises.Promise;
import org.igarape.copcast.promises.PromiseError;
import org.igarape.copcast.promises.PromisePayload;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.EditTextUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.LocationUtils;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.OkDialog;
import org.igarape.copcast.utils.StateManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.view.Gravity.CENTER_HORIZONTAL;

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
        txtPwd = (EditText) findViewById(R.id.txtLoginPassword);
        Button btnLoginOk = (Button) findViewById(R.id.btn_login_ok);

        EditTextUtils.showKeyboardOnFocusAndClick(this, txtId);

        btnLoginOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeLoginRequest(v);
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String msg = extras.getString("reason");
            if (msg != null && msg.length() > 0) {
                Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | CENTER_HORIZONTAL, 0, 100);
                toast.show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (txtId.isFocused()) {
            Log.d(TAG, "focused on id");
            EditTextUtils.showKeyboard(this, txtId);
        } else if (txtPwd.isFocused()) {
            Log.d(TAG, "focused on pwd");
            EditTextUtils.showKeyboard(this, txtPwd);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.action_settings:
                i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.device_registration:
                i = new Intent(this, RegistrationActivity.class);
                startActivity(i);
                break;
        }

        return true;
    }

    public void makeLoginRequest(View view) {
        if (!LocationUtils.isHighAccuracyLocationEnabled(this)) {
            LocationUtils.showHighAccuracyLocationDisabledAlert(this);
            return;
        }

        pDialog = new ProgressDialog(this);
        pDialog.setTitle(getString(R.string.login_in));
        pDialog.setMessage(getString(R.string.please_hold));
        pDialog.setIndeterminate(true);
        pDialog.show();

        final String loginField = txtId.getText().toString().trim();
        final String passwordField = txtPwd.getText().toString();

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] args) {
                final List<Pair<String, String>> params = new ArrayList();

                params.add(new Pair("username", loginField));
                params.add(new Pair("password", passwordField));
                params.add(new Pair("scope", "client"));

                NetworkUtils.post(getApplicationContext(), "/token", params, new Promise() {
                    @Override
                    public void success(PromisePayload payload) {
                        JSONObject response = (JSONObject) payload.get("response");
                        Log.d(TAG, "@JSONRESPONSE=[" + response + "]");
                        String token = null;
                        try {
                            token = (String) response.get("token");
                            Globals.setUserName(getApplicationContext(), response.getString("userName"));
                            Globals.setUserId(getApplicationContext(), response.getInt("userId"));
                            Globals.setHasVideoPlayback(getApplicationContext(), response.getBoolean("hasVideoPlayback"));
                            Globals.setShowFeedback(getApplicationContext(), response.getBoolean("showFeedback"));
                        } catch (JSONException e) {
                            OkDialog.displayAndTerminate(LoginActivity.this, getString(R.string.warning), getString(R.string.internal_error));
                            Log.e(TAG, "error on login", e);
                            return;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (pDialog != null) {
                                    pDialog.dismiss();
                                    pDialog = null;
                                }
                            }
                        });
                        Globals.setAccessToken(getBaseContext(), token);
                        Globals.setUserLogin(getBaseContext(), loginField);

                        StateManager.setStateOrDie(LoginActivity.this, State.IDLE);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        LoginActivity.this.finish();
                    }

                    @Override
                    public void error(PromiseError error) {
                        switch((HttpPromiseError) error) {
                            case NOT_AUTHORIZED:
                                showToast(R.string.unauthorized_login);
                                break;
                            case FORBIDDEN:
                                showToast(R.string.forbidden_login);
                                break;
                            case NO_CONNECTION:
                                showToast(R.string.network_required);
                                break;
                            case BAD_CONNECTION:
                                showToast(R.string.connection_error);
                                break;
                            case BAD_REQUEST:
                            case BAD_RESPONSE:
                                showToast(R.string.bad_request_error);
                                break;
                            case FAILURE:
                                showToast(R.string.server_error);
                                break;
                        }
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
                });
                return null;
            }

        }.execute(null, null, null);
    }
}
