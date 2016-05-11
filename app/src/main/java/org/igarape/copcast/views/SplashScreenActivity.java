package org.igarape.copcast.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;
//import com.splunk.mint.Mint;
import org.igarape.copcast.R;
import org.igarape.copcast.service.sign.SigningService;
import org.igarape.copcast.service.sign.SigningServiceException;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.StateManager;

public class SplashScreenActivity extends Activity {

    public static String TAG = SplashScreenActivity.class.getName();
    private static final int SPLASH_SHOW_TIME = 2500;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        Globals.sessionInit();
        Globals.initStateManager(this);
        FileUtils.init(getApplicationContext());
        Globals.setAccessToken(this, null);
        Globals.setDirectorySize(getApplicationContext(), FileUtils.getDirectorySize());


        //splunk initialization
//        Mint.initAndStartSession(SplashScreenActivity.this, "0c1e5146");
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

        // verify if we already have the signing mechanism initialized.
        // if not, prompt the user for server and credentials.
        String server_url = Globals.getServerUrl(this);
        String registered_server_url = Globals.getAppRegistered(this);
        if (registered_server_url == null || registered_server_url.compareTo(server_url)!=0) {
            Intent intent = new Intent(SplashScreenActivity.this, RegistrationActivity.class);
            startActivity(intent);
            SplashScreenActivity.this.finish();
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    SigningService.loadIDs(SplashScreenActivity.this);
                } catch (SigningServiceException e) {
                    ILog.e(TAG, "Failed to load device ID parameters", e);
                    Toast.makeText(SplashScreenActivity.this, getString(R.string.error_keystore), Toast.LENGTH_LONG);
                    SplashScreenActivity.this.finish();
                }

                if (Globals.getAccessToken(getApplicationContext()) instanceof String){
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    StateManager.setStateOrDie(SplashScreenActivity.this, State.IDLE);
                    startActivity(intent);
                    SplashScreenActivity.this.finish();
                }

                startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
                SplashScreenActivity.this.finish();

            }
        }, SPLASH_SHOW_TIME);
    }
}
