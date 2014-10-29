package org.igarape.copcast.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.igarape.copcast.R;

import org.igarape.copcast.utils.Globals;

import java.io.IOException;

public class SplashScreenActivity extends Activity {

	private static final int SPLASH_SHOW_TIME = 5000;
    public static String TAG = SplashScreenActivity.class.getName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private Context context;
    private GoogleCloudMessaging gcm;
    private String regid = null;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_spash_screen);

        context = getApplicationContext();
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = Globals.getRegistrationId(context);
        }
        Globals.setAccessToken(this, null);
		new BackgroundSplashTask().execute();
	}

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
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
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                if (regid.isEmpty()) {
                    regid = gcm.register(Globals.SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    Globals.storeRegistrationId(context, regid);
                }
            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
                // If there is an error, don't just keep trying to register.
                // Require the user to click a button again, or perform
                // exponential back-off.
            }
			// I have just given a sleep for this thread
			// if you want to load database, make
			// network calls, load images
			// you can do them here and remove the following
			// sleep

			// do not worry about this Thread.sleep
			// this is an async task, it will not disrupt the UI

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