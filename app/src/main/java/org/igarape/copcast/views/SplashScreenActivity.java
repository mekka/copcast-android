package org.igarape.copcast.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.splunk.mint.Mint;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.BatteryUtils;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;

public class SplashScreenActivity extends Activity {

    private static final int SPLASH_SHOW_TIME = 5000;
    public static String TAG = SplashScreenActivity.class.getName();
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private Context context;
    private String regid = null;  //testing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mint.initAndStartSession(SplashScreenActivity.this, "0c1e5146");

        if (Globals.getAccessToken(getApplicationContext()) instanceof String){
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
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

            regid = Globals.getRegistrationId(context);
            Globals.setAccessToken(this, null);
            new BackgroundSplashTask().execute();
        }
    }

    private void queryBatteryStatus(){
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        BatteryUtils.getSingletonInstance().updateValues(batteryStatus);
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
            String msg = null;

            // I have just given a sleep for this thread
            // if you want to load database, make
            // network calls, load images
            // you can do them here and remove the following
            // sleep

            // do not worry about this Thread.sleep
            // this is an async task, it will not disrupt the UI
            queryBatteryStatus();
            FileUtils.init(getApplicationContext());
            Globals.setDirectorySize(getApplicationContext(), FileUtils.getDirectorySize());
            try {
                Thread.sleep(SPLASH_SHOW_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return msg;
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
