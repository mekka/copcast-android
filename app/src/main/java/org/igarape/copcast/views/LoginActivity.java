package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.igarape.copcast.R;
import org.igarape.copcast.utils.ApiClient;
import org.igarape.copcast.utils.Globals;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    public static String TAG = LoginActivity.class.getName();
    RequestQueue queue;
    EditText txtId;
    EditText txtPwd;
    ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtId = (EditText) findViewById(R.id.txtLoginUser);
        txtId.setText(Globals.getUserLogin(this));

        txtPwd = (EditText) findViewById(R.id.txtLoginPassword);

        queue = Volley.newRequestQueue(this);
        ((Button) findViewById(R.id.btn_login_ok)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeLoginRequest();
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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void makeLoginRequest() {
        RequestParams params = new RequestParams();
        params.put("username", txtId.getText().toString());
        params.put("password", txtPwd.getText().toString());
        params.put("scope", "client");
        params.put("gcm_registration", Globals.getRegistrationId(getApplicationContext()));

        pDialog = ProgressDialog.show(this, "Fazendo login", "Por favor aguarde...", true);

        ApiClient.post("/token", params, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseBody, Throwable e) {
                if (pDialog != null) {
                    pDialog.dismiss();
                    pDialog = null;
                }
                if (statusCode == 401) {
                    Toast.makeText(LoginActivity.this, LoginActivity.this.getString(R.string.unauthorized_login), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "Error: " + responseBody, e);
                    Toast.makeText(LoginActivity.this, LoginActivity.this.getString(R.string.no_server_login), Toast.LENGTH_LONG).show();
                }
            }

            private void successResponse(JSONObject response) {
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

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseBody) {
                try {
                    successResponse(new JSONObject(responseBody));
                } catch (JSONException e) {
                    Log.e(TAG, "error on login", e);
                }

            }
        });
    }

}
