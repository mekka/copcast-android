package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.splunk.mint.Mint;

import org.igarape.copcast.R;
import org.igarape.copcast.service.sign.SigningService;
import org.igarape.copcast.service.sign.SigningServiceException;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.BatteryUtils;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.StateManager;

import java.security.KeyStore;

public class SplashScreenActivity extends Activity {

    private static final int SPLASH_SHOW_TIME = 5000;
    public static String TAG = SplashScreenActivity.class.getName();
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //splunk initialization
        Mint.initAndStartSession(SplashScreenActivity.this, "0c1e5146");

        // verify if we already have the signing mechanism initialized.
        // if not, prompt the user for server and credentials.
        String server_url = Globals.getServerUrl(this);
        String registered_server_url = Globals.getAppRegistered(this);
        if (registered_server_url == null || registered_server_url.compareTo(server_url)!=0) {
            Intent intent = new Intent(SplashScreenActivity.this, RegistrationActivity.class);
            startActivity(intent);
            SplashScreenActivity.this.finish();
        }


        try {
            SigningService.loadIDs(this);
        } catch (SigningServiceException e) {
            ILog.e(TAG, "Failed to load device ID parameters", e);
            Toast.makeText(this, getString(R.string.error_keystore), Toast.LENGTH_LONG);
            SplashScreenActivity.this.finish();
        }


        if (Globals.getAccessToken(getApplicationContext()) instanceof String){
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            StateManager.setStateOrDie(this, State.IDLE);
            startActivity(intent);
            SplashScreenActivity.this.finish();
            return;
        }

        setContentView(R.layout.activity_splash_screen);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        context = getApplicationContext();
        if (checkPlayServices()) {
            Globals.setAccessToken(this, null);
            new BackgroundSplashTask().execute();
        }
    }

    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                Toast.makeText(this, "This device is not supported.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this,
                REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RECOVER_PLAY_SERVICES:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Google Play Services must be installed.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Async Task: can be used to load DB, images during which the splash screen
     * is shown to user
     */
    private class BackgroundSplashTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... arg0) {

            // I have just given a sleep for this thread
            // if you want to load database, make
            // network calls, load images
            // you can do them here and remove the following
            // sleep

            // do not worry about this Thread.sleep
            // this is an async task, it will not disrupt the UI
            FileUtils.init(getApplicationContext());
            Globals.setDirectorySize(getApplicationContext(), FileUtils.getDirectorySize());
            return null;
        }

        @Override
        protected void onPostExecute(String msg) {
            super.onPostExecute(msg);

            PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

            Intent i = new Intent(SplashScreenActivity.this,
                    LoginActivity.class);
            // any info loaded can during splash_show
            // can be passed to main activity using
            // below
            i.putExtra("loaded_info", " ");
            startActivity(i);
            finish();
        }

    }
}
