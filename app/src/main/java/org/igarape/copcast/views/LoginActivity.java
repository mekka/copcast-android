package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.igarape.copcast.R;
import org.igarape.copcast.utils.ApiClient;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                    },200);
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
        if (hasErrors() || !hasConnection()) return;
        pDialog = ProgressDialog.show(this, "Fazendo login", "Por favor aguarde...", true);
        new LoginTask().execute();
    }

    private class LoginTask extends AsyncTask<Void, Void, Void> {

        private JSONObject response;

        @Override
        protected Void doInBackground(Void... unused) {
            final String regId = Globals.getRegistrationId(getApplicationContext());

            response = NetworkUtils.postRequest(txtId.getText().toString(), txtPwd.getText().toString(), regId);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            Log.d(TAG, "@JSONRESPONSE=[" + response + "]");
            String token = null;
            try {
                token = (String) response.get("token");
                String ipAddress = (String) response.get("ipAddress");
                if (ipAddress != null) {
                    Globals.setServerIpAddress(ipAddress);
                }
            } catch (JSONException e) {
                Log.e(TAG, "error on login", e);
            }

            try {
                Globals.setStreamingPort(Integer.parseInt((String) response.get("streamingPort")));
                Globals.setStreamingUser((String) response.get("streamingUser"));
                Globals.setStreamingPassword((String) response.get("streamingPassword"));
                Globals.setStreamingPath((String) response.get("streamingPath"));
                Globals.setUserName((String) response.get("userName"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (pDialog != null) {
                pDialog.dismiss();
                pDialog = null;
            }
            Globals.setAccessToken(getBaseContext(), token);
            Globals.setUserLogin(getBaseContext(), txtId.getText().toString());
            ApiClient.setToken(token);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            LoginActivity.this.finish();
        }
    }

    private void old(){
        final String regId = Globals.getRegistrationId(getApplicationContext());
        RequestParams params = new RequestParams();
        params.put("username", txtId.getText().toString());
        params.put("password", txtPwd.getText().toString());
        params.put("scope", "client");
        params.put("gcm_registration", regId);

        pDialog = ProgressDialog.show(this, "Fazendo login", "Por favor aguarde...", true);

        ApiClient.post("/token", params, new JsonHttpResponseHandler() {

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                onError(statusCode, throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                onError(statusCode, throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onError(statusCode, throwable);
            }

            private void onError(int statusCode, Throwable e) {
                Log.e(TAG, "onFailure: statusCode=[" + statusCode + "]");
                if (pDialog != null) {
                    pDialog.dismiss();
                    pDialog = null;
                }
                if (statusCode == 401) {
                    Toast.makeText(LoginActivity.this, LoginActivity.this.getString(R.string.unauthorized_login), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "Error: ",e);
                    Toast.makeText(LoginActivity.this, LoginActivity.this.getString(R.string.no_server_login), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d(TAG, "@JSONRESPONSE=[" + response + "]");
                String token = null;
                try {
                    token = (String) response.get("token");
                    String ipAddress = (String) response.get("ipAddress");
                    if (ipAddress != null) {
                        Globals.setServerIpAddress(ipAddress);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "error on login", e);
                }
                if (pDialog != null) {
                    pDialog.dismiss();
                    pDialog = null;
                }
                try {
                    Globals.setStreamingPort(Integer.parseInt((String) response.get("streamingPort")));
                    Globals.setStreamingUser((String) response.get("streamingUser"));
                    Globals.setStreamingPassword((String) response.get("streamingPassword"));
                    Globals.setStreamingPath((String) response.get("streamingPath"));
                    Globals.setUserName((String) response.get("userName"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Globals.setAccessToken(getBaseContext(), token);
                Globals.setUserLogin(getBaseContext(), txtId.getText().toString());
                ApiClient.setToken(token);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                LoginActivity.this.finish();
            }
        });
    }

    private boolean hasErrors() {
        final String login = txtId.getText().toString();
        final String password = txtPwd.getText().toString();
        if(null == login || login.isEmpty()){
            Log.d(TAG, "login required");
            Toast toast = Toast.makeText(getApplicationContext(), R.string.login_required, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 100);
            toast.show();
            return true;
        }
        if(null == password || password.isEmpty()){
            Log.d(TAG,"password required");
            Toast toast = Toast.makeText(getApplicationContext(), R.string.password_required, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 100);
            toast.show();
            return true;
        }
        return false;
    }

    private boolean hasConnection(){
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            Log.d(TAG, "network required");
            Toast toast = Toast.makeText(getApplicationContext(), R.string.network_required, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 100);
            toast.show();
            return false;
        }
    }
}
