package com.igarape.copcast.views;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.igarape.copcast.R;
import com.igarape.copcast.utils.Globals;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class SplashScreenActivity extends Activity {

	private static final int SPLASH_SHOW_TIME = 5000;
    public static String TAG = SplashScreenActivity.class.getName();
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
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
            regid = getRegistrationId(context);
        }
        Globals.setAccessToken(this, null);
		new BackgroundSplashTask().execute();
	}

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences("SPLASH", Context.MODE_PRIVATE);
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
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

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
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
                regid = gcm.register(Globals.SENDER_ID);
                msg = "Device registered, registration ID=" + regid;
                if (regid.isEmpty()) {
                    storeRegistrationId(context, regid);
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